package net.mysterria.voting.menu;

import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditOutcome;
import net.mysterria.voting.audit.ClickAuditLimiter;
import net.mysterria.voting.audit.VotingAuditEmitter;
import net.mysterria.voting.claims.VoteClaimStore;
import net.mysterria.voting.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Console commands are the reward and run at most once per player per service: the claim is
 * persisted before they are dispatched. Player commands, messages and titles run on every click.
 */
public final class VoteClickActions {
    private final VoteClaimStore claims;
    private final Supplier<VotingAuditEmitter> audit;
    private final ClickAuditLimiter limiter = new ClickAuditLimiter();

    public VoteClickActions(VoteClaimStore claims, Supplier<VotingAuditEmitter> audit) {
        this.claims = claims;
        this.audit = audit;
    }

    public void execute(MenuClick click) {
        FileConfiguration config = click.langConfig();
        String path = click.actionsPath();
        if (!config.contains(path)) return;
        Player p = click.player();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("target", p.getName());
        List<String> consoleCmds = config.contains(path + ".run-command.console")
                ? config.getStringList(path + ".run-command.console") : List.of();

        RewardState state = claimReward(p.getUniqueId(), click.service(), !consoleCmds.isEmpty());
        VoteClickTrail trail = new VoteClickTrail(click, UUID.randomUUID(),
                shouldAudit(p.getUniqueId(), click.service(), state), audit, claims);
        trail.emitClick(state.reason);

        if (config.contains(path + ".run-command.player")) {
            runPlayerCommands(trail, config.getStringList(path + ".run-command.player"), placeholders);
        }
        if (state == RewardState.CLAIMED) {
            trail.emitClaim(AuditOutcome.COMMITTED, null, consoleCmds.size());
            dispatchConsoleCommands(trail, consoleCmds, placeholders);
        } else if (state != RewardState.NO_REWARD) {
            trail.emitClaim(state == RewardState.PERSIST_FAILED ? AuditOutcome.FAILED : AuditOutcome.DENIED,
                    state.reason, consoleCmds.size());
        }
        sendFeedback(p, config, path, placeholders);
        p.closeInventory();
    }

    private RewardState claimReward(UUID playerId, String service, boolean hasReward) {
        if (!hasReward) return RewardState.NO_REWARD;
        if (claims.claimedAt(playerId, service) != null) return RewardState.ALREADY_CLAIMED;
        if (!claims.isAvailable()) return RewardState.CLAIMS_UNAVAILABLE;
        return claims.claim(playerId, service, System.currentTimeMillis())
                ? RewardState.CLAIMED : RewardState.PERSIST_FAILED;
    }

    /** Reward and failed-claim clicks are always audited; the rest once per window. */
    private boolean shouldAudit(UUID playerId, String service, RewardState state) {
        if (state == RewardState.CLAIMED || state == RewardState.PERSIST_FAILED) {
            limiter.touch(playerId, service);
            return true;
        }
        return limiter.allow(playerId, service);
    }

    private static void runPlayerCommands(VoteClickTrail trail, List<String> commands,
                                          Map<String, String> placeholders) {
        for (String cmd : commands) {
            String formattedCmd = MessageUtils.formatPlain(cmd, placeholders);
            boolean result = trail.player().performCommand(formattedCmd);
            trail.emitPlayerCommand(cmd, formattedCmd, result);
        }
    }

    private static void dispatchConsoleCommands(VoteClickTrail trail, List<String> commands,
                                                Map<String, String> placeholders) {
        for (int i = 0; i < commands.size(); i++) {
            String cmd = commands.get(i);
            String formattedCmd = MessageUtils.formatPlain(cmd, placeholders);
            boolean result;
            try {
                result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
            } catch (RuntimeException failure) {
                trail.emitDispatch(i, commands.size(), cmd, formattedCmd, false, failure.getClass().getSimpleName());
                throw failure;
            }
            trail.emitDispatch(i, commands.size(), cmd, formattedCmd, result, null);
        }
    }

    private static void sendFeedback(Player p, FileConfiguration config, String path,
                                     Map<String, String> placeholders) {
        if (config.contains(path + ".message")) {
            for (String msg : config.getStringList(path + ".message")) {
                p.sendMessage(MessageUtils.formatMessage(msg, placeholders));
            }
        }
        if (config.contains(path + ".title")) {
            String titleText = config.getString(path + ".title.title");
            String subtitleText = config.getString(path + ".title.subtitle");
            if (titleText != null || subtitleText != null) {
                MessageUtils.sendTitle(p, titleText, subtitleText, placeholders);
            }
        }
    }

    private enum RewardState {
        NO_REWARD("no_reward"),
        CLAIMED("claimed"),
        ALREADY_CLAIMED("already_claimed"),
        CLAIMS_UNAVAILABLE("claims_unavailable"),
        PERSIST_FAILED("persist_failed");

        final String reason;

        RewardState(String reason) {
            this.reason = reason;
        }
    }
}

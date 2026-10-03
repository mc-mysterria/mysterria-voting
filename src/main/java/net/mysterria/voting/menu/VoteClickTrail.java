package net.mysterria.voting.menu;

import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditOutcome;
import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditRisk;
import net.mysterria.voting.audit.AuditContext;
import net.mysterria.voting.audit.VotingAuditEmitter;
import net.mysterria.voting.audit.VotingAuditEmitter.AuditRow;
import net.mysterria.voting.claims.VoteClaimStore;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * When the click is rate-limited ({@code audited == false}) only reward dispatch rows are written,
 * and those only occur on a successful claim, which is never rate-limited. Rows are built inside
 * the emitter's guard, so a failure while collecting metadata never affects the click.
 */
final class VoteClickTrail {
    static final String MENU_KEY = "voting";

    private final MenuClick click;
    private final UUID correlationId;
    private final boolean audited;
    private final Supplier<VotingAuditEmitter> audit;
    private final VoteClaimStore claims;

    VoteClickTrail(MenuClick click, UUID correlationId, boolean audited,
                   Supplier<VotingAuditEmitter> audit, VoteClaimStore claims) {
        this.click = click;
        this.correlationId = correlationId;
        this.audited = audited;
        this.audit = audit;
        this.claims = claims;
    }

    Player player() {
        return click.player();
    }

    void emitClick(String rewardState) {
        if (!audited) return;
        send(() -> {
            Map<String, Object> metadata = base();
            metadata.put("raw_slot", click.rawSlot());
            metadata.put("clicked_inventory", click.clickedSide());
            metadata.put("inventory_holder_type", click.holderType());
            metadata.put("view_title", click.viewTitle());
            metadata.put("reward_state", rewardState);
            AuditContext.putItem(metadata, click.item());
            return row("voting.menu.click_action", AuditOutcome.OBSERVED, AuditRisk.LOW, click.service(), null, metadata);
        });
    }

    void emitClaim(AuditOutcome outcome, String reason, int commandCount) {
        if (!audited) return;
        send(() -> {
            Map<String, Object> metadata = base();
            metadata.put("command_count", commandCount);
            if (reason != null) metadata.put("reason", reason);
            Long claimedAt = claims.claimedAt(click.player().getUniqueId(), click.service());
            if (claimedAt != null) metadata.put("claimed_at", claimedAt);
            String event = outcome == AuditOutcome.DENIED ? "voting.reward.claim_denied" : "voting.reward.claimed";
            AuditRisk risk = outcome == AuditOutcome.FAILED ? AuditRisk.HIGH : AuditRisk.NORMAL;
            return row(event, outcome, risk, claimBusinessId(), reason, metadata);
        });
    }

    void emitPlayerCommand(String template, String resolved, boolean result) {
        if (!audited) return;
        send(() -> {
            Map<String, Object> metadata = base();
            metadata.put("command_template", template);
            metadata.put("command_resolved", resolved);
            metadata.put("result", result);
            return row("voting.reward.player_command", result ? AuditOutcome.COMMITTED : AuditOutcome.FAILED,
                    AuditRisk.NORMAL, click.service(), null, metadata);
        });
    }

    void emitDispatch(int index, int count, String template, String resolved, boolean result, String error) {
        send(() -> {
            Map<String, Object> metadata = base();
            metadata.put("command_index", index);
            metadata.put("command_count", count);
            metadata.put("command_template", template);
            metadata.put("command_resolved", resolved);
            metadata.put("dispatch_result", result);
            if (error != null) metadata.put("error", error);
            AuditRisk risk = RewardCommandRisk.isHighRisk(template) ? AuditRisk.HIGH : AuditRisk.NORMAL;
            return row("voting.reward.command_dispatched", result ? AuditOutcome.COMMITTED : AuditOutcome.FAILED,
                    risk, claimBusinessId(), error, metadata);
        });
    }

    private String claimBusinessId() {
        return click.player().getUniqueId() + ":" + click.service();
    }

    private Map<String, Object> base() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("menu_key", MENU_KEY);
        metadata.put("service", click.service());
        metadata.put("menu_lang", click.lang());
        metadata.put("slot", click.slot());
        metadata.put("click_type", click.clickType());
        metadata.put("actor_type", "player");
        AuditContext.putLocation(metadata, click.player());
        return metadata;
    }

    private AuditRow row(String event, AuditOutcome outcome, AuditRisk risk, String businessId, String reason,
                         Map<String, Object> metadata) {
        return new AuditRow(event, outcome, risk, correlationId, businessId,
                click.player().getUniqueId(), null, reason, metadata);
    }

    private void send(Supplier<AuditRow> row) {
        VotingAuditEmitter emitter = audit.get();
        if (emitter != null) emitter.emit(row);
    }
}

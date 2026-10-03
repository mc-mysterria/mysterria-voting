package net.mysterria.voting.audit;

import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditOutcome;
import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditRisk;
import net.mysterria.voting.audit.VotingAuditEmitter.AuditRow;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Main thread only. Rows are built inside the emitter's guard and never affect the command. */
public final class AdminAudit {
    private final Supplier<VotingAuditEmitter> audit;

    public AdminAudit(Supplier<VotingAuditEmitter> audit) {
        this.audit = audit;
    }

    public static ConfigFingerprint fingerprint(Supplier<ConfigFingerprint> source) {
        try {
            return source.get();
        } catch (RuntimeException failure) {
            return new ConfigFingerprint(null, null, 0, "unavailable", -1);
        }
    }

    public void reloaded(CommandSender sender, String scope, ConfigFingerprint before, ConfigFingerprint after) {
        send(() -> {
            Map<String, Object> metadata = actor(sender);
            metadata.put("scope", scope);
            boolean hashed = before.menuHash() != null && after.menuHash() != null;
            if (hashed) {
                metadata.put("menu_actions_hash_before", before.menuHash());
                metadata.put("menu_actions_hash_after", after.menuHash());
                metadata.put("menu_actions_changed", !before.menuHash().equals(after.menuHash()));
                metadata.put("console_command_templates", after.consoleTemplates());
                metadata.put("console_command_template_count", after.consoleTemplateCount());
            }
            metadata.put("reminders_hash_before", before.remindersHash());
            metadata.put("reminders_hash_after", after.remindersHash());
            metadata.put("reminders_changed", !before.remindersHash().equals(after.remindersHash()));
            metadata.put("reminder_count_before", before.reminderCount());
            metadata.put("reminder_count_after", after.reminderCount());
            // A voting reload whose menu actions changed (or could not be hashed) can change rewards.
            boolean rewardsMayChange = scope.equals("voting")
                    && (!hashed || !before.menuHash().equals(after.menuHash()));
            return row("voting.admin.reload", rewardsMayChange ? AuditRisk.HIGH : AuditRisk.NORMAL, scope,
                    sender, null, metadata);
        });
    }

    public void broadcast(CommandSender sender, int recipientCount) {
        send(() -> {
            Map<String, Object> metadata = actor(sender);
            metadata.put("recipient_count", recipientCount);
            return row("voting.admin.broadcast", AuditRisk.LOW, "vote_broadcast", sender, null, metadata);
        });
    }

    public void reminderSent(CommandSender sender, String reminderId, Player target, int onlineCount) {
        send(() -> {
            Map<String, Object> metadata = actor(sender);
            metadata.put("reminder_id", reminderId);
            metadata.put("mode", target == null ? "all" : "player");
            if (target == null) {
                metadata.put("online_count", onlineCount);
            }
            return row("voting.admin.reminder_sent", AuditRisk.LOW, reminderId, sender,
                    target == null ? null : target.getUniqueId(), metadata);
        });
    }

    private static Map<String, Object> actor(CommandSender sender) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        AuditContext.putActor(metadata, sender);
        AuditContext.putLocation(metadata, sender);
        return metadata;
    }

    private static AuditRow row(String event, AuditRisk risk, String businessId, CommandSender sender,
                                UUID subjectId, Map<String, Object> metadata) {
        return new AuditRow(event, AuditOutcome.COMMITTED, risk, UUID.randomUUID(), businessId,
                AuditContext.actorId(sender), subjectId, null, metadata);
    }

    private void send(Supplier<AuditRow> row) {
        VotingAuditEmitter emitter = audit.get();
        if (emitter != null) {
            emitter.emit(row);
        }
    }
}

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

    public void reloaded(CommandSender sender, String scope) {
        send(() -> {
            Map<String, Object> metadata = actor(sender);
            metadata.put("scope", scope);
            // A voting reload can change the menu reward commands.
            return row("voting.admin.reload", AuditOutcome.COMMITTED,
                    scope.equals("voting") ? AuditRisk.HIGH : AuditRisk.NORMAL, scope, sender, null, null, metadata);
        });
    }

    public void reloadFailed(CommandSender sender, String scope, RuntimeException failure) {
        String error = failure.getClass().getSimpleName() + ": " + failure.getMessage();
        send(() -> {
            Map<String, Object> metadata = actor(sender);
            metadata.put("scope", scope);
            metadata.put("error", error);
            return row("voting.admin.reload", AuditOutcome.FAILED, AuditRisk.HIGH, scope, sender, null, error,
                    metadata);
        });
    }

    public void broadcast(CommandSender sender, int recipientCount) {
        send(() -> {
            Map<String, Object> metadata = actor(sender);
            metadata.put("recipient_count", recipientCount);
            return row("voting.admin.broadcast", AuditOutcome.COMMITTED, AuditRisk.LOW, "vote_broadcast", sender,
                    null, null, metadata);
        });
    }

    public void reminderSent(CommandSender sender, String reminderId, Player target) {
        send(() -> {
            Map<String, Object> metadata = actor(sender);
            metadata.put("reminder_id", reminderId);
            metadata.put("mode", target == null ? "all" : "player");
            return row("voting.admin.reminder_sent", AuditOutcome.COMMITTED, AuditRisk.LOW, reminderId, sender,
                    target == null ? null : target.getUniqueId(), null, metadata);
        });
    }

    private static Map<String, Object> actor(CommandSender sender) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        AuditContext.putActor(metadata, sender);
        AuditContext.putLocation(metadata, sender);
        return metadata;
    }

    private static AuditRow row(String event, AuditOutcome outcome, AuditRisk risk, String businessId,
                                CommandSender sender, UUID subjectId, String reason, Map<String, Object> metadata) {
        return new AuditRow(event, outcome, risk, UUID.randomUUID(), businessId,
                AuditContext.actorId(sender), subjectId, reason, metadata);
    }

    private void send(Supplier<AuditRow> row) {
        VotingAuditEmitter emitter = audit.get();
        if (emitter != null) {
            emitter.emit(row);
        }
    }
}

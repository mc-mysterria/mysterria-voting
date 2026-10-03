package net.mysterria.voting.audit;

import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditOutcome;
import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditPrivacy;
import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditProducer;
import dev.ua.ikeepcalm.mysterria.audit.client.api.AuditRisk;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class VotingAuditEmitter implements AutoCloseable {
    private static final int MAX_TEXT = 256;
    private static final int MAX_LONG_TEXT = 1_024;
    private static final int MAX_KEYS = 48;

    /** Null when the audit client failed to initialise; every call is then a no-op. */
    private final AuditProducer producer;

    public VotingAuditEmitter(JavaPlugin plugin) {
        this.producer = createProducer(plugin);
    }

    private static AuditProducer createProducer(JavaPlugin plugin) {
        try {
            return AuditProducer.create(plugin.getDataFolder().toPath().toAbsolutePath().getParent()
                            .resolve("mysterria-audit-spool"),
                    "mysterria-voting", plugin.getPluginMeta().getVersion());
        } catch (RuntimeException | LinkageError failure) {
            plugin.getLogger().warning("Audit client unavailable; voting audit events are disabled: " + failure);
            return null;
        }
    }

    /**
     * Never throws, including when the row builder fails; audit delivery must not gate rewards,
     * menus or commands. The builder runs synchronously on the caller's (main) thread.
     */
    public void emit(Supplier<AuditRow> source) {
        if (producer == null || source == null) {
            return;
        }
        try {
            AuditRow row = source.get();
            if (row == null) return;
            producer.emit(row.eventType(), row.outcome(), row.risk(), AuditPrivacy.STAFF_RESTRICTED,
                    row.correlationId() == null ? UUID.randomUUID() : row.correlationId(),
                    row.businessId() == null ? null : bounded(row.businessId(), MAX_TEXT),
                    row.actorId(), row.subjectId(), null,
                    row.reason() == null ? null : bounded(row.reason(), MAX_TEXT),
                    boundedMetadata(row.metadata()));
        } catch (RuntimeException | LinkageError failure) {
            recordFailure();
        }
    }

    private void recordFailure() {
        try {
            producer.recordFailure();
        } catch (RuntimeException | LinkageError ignored) {
            // Failure accounting is itself best effort.
        }
    }

    private static Map<String, Object> boundedMetadata(Map<String, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && result.size() < MAX_KEYS) {
                result.put(bounded(key, 64), boundedValue(key, value));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private static Object boundedValue(String key, Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        int limit = key.startsWith("command") || key.endsWith("_templates") ? MAX_LONG_TEXT : MAX_TEXT;
        return bounded(value instanceof String text ? text : String.valueOf(value), limit);
    }

    private static String bounded(String value, int limit) {
        if (value.codePointCount(0, value.length()) <= limit) return value;
        return value.substring(0, value.offsetByCodePoints(0, limit));
    }

    @Override
    public void close() {
        if (producer == null) {
            return;
        }
        try {
            producer.close();
        } catch (RuntimeException | LinkageError ignored) {
            // Shutdown must continue even if the audit client cannot flush.
        }
    }

    /** One audit row; {@code metadata} values must be plain values captured on the main thread. */
    public record AuditRow(String eventType, AuditOutcome outcome, AuditRisk risk, UUID correlationId,
                           String businessId, UUID actorId, UUID subjectId, String reason,
                           Map<String, ?> metadata) {
    }
}

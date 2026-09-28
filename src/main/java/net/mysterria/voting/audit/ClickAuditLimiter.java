package net.mysterria.voting.audit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps repeat menu clicks from producing one audit row per click. Clicks that dispatch a
 * reward are always audited; other clicks are audited at most once per player per service
 * per window. Main thread only; in-memory, resets on restart.
 */
public final class ClickAuditLimiter {
    private static final long WINDOW_MILLIS = 5L * 60L * 1000L;
    private static final int MAX_ENTRIES = 1_024;

    private final Map<String, Long> lastAudited = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    /** Returns true and records the click when the player/service pair is outside its window. */
    public boolean allow(UUID playerId, String service) {
        String key = playerId + ":" + service;
        long now = System.currentTimeMillis();
        Long previous = lastAudited.get(key);
        if (previous != null && now - previous < WINDOW_MILLIS) {
            return false;
        }
        lastAudited.put(key, now);
        return true;
    }

    /** Records an always-audited click so the next non-reward click starts a fresh window. */
    public void touch(UUID playerId, String service) {
        lastAudited.put(playerId + ":" + service, System.currentTimeMillis());
    }
}

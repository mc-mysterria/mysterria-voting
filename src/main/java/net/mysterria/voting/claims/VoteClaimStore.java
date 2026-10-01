package net.mysterria.voting.claims;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persisted record of which vote-menu services each player has already claimed a console
 * reward from. A claim is written to {@code claims.yml} before the reward is dispatched, so a
 * crash after the write can only lose a reward, never duplicate one. Main thread only.
 */
public final class VoteClaimStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, Long>> claims = new HashMap<>();
    /** False when claims.yml exists but could not be parsed; claims are then refused. */
    private boolean available;

    public VoteClaimStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "claims.yml");
        load();
    }

    private void load() {
        claims.clear();
        available = true;
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException failure) {
            available = false;
            plugin.getLogger().log(Level.SEVERE, "Could not read claims.yml; vote rewards are paused until it is fixed", failure);
            return;
        }
        ConfigurationSection root = yaml.getConfigurationSection("claims");
        if (root == null) {
            return;
        }
        for (String playerKey : root.getKeys(false)) {
            ConfigurationSection services = root.getConfigurationSection(playerKey);
            UUID playerId = parseUuid(playerKey);
            if (services == null || playerId == null) {
                continue;
            }
            Map<String, Long> playerClaims = claims.computeIfAbsent(playerId, id -> new HashMap<>());
            for (String service : services.getKeys(false)) {
                playerClaims.put(service, services.getLong(service));
            }
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /** Epoch millis of the player's claim for a service, or null when unclaimed. */
    public Long claimedAt(UUID playerId, String service) {
        Map<String, Long> playerClaims = claims.get(playerId);
        return playerClaims == null ? null : playerClaims.get(service);
    }

    /**
     * Records and persists a claim. Returns false, leaving no claim in memory, when the store
     * is unavailable, the service is already claimed, or the write fails.
     */
    public boolean claim(UUID playerId, String service, long claimedAt) {
        if (!available || claimedAt(playerId, service) != null) {
            return false;
        }
        Map<String, Long> playerClaims = claims.computeIfAbsent(playerId, id -> new HashMap<>());
        playerClaims.put(service, claimedAt);
        try {
            save();
            return true;
        } catch (IOException failure) {
            playerClaims.remove(service);
            if (playerClaims.isEmpty()) {
                claims.remove(playerId);
            }
            plugin.getLogger().log(Level.SEVERE, "Could not save claims.yml; vote reward not dispensed", failure);
            return false;
        }
    }

    private void save() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        claims.forEach((playerId, services) ->
                services.forEach((service, at) -> yaml.set("claims." + playerId + "." + service, at)));
        Path target = file.toPath();
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling("claims.yml.tmp");
        Files.writeString(temp, yaml.saveToString(), StandardCharsets.UTF_8);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }
}

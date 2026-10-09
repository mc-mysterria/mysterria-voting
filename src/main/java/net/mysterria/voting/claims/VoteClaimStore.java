package net.mysterria.voting.claims;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persisted record of the vote-menu services each player has already claimed a reward from,
 * stored in {@code claims.yml} as {@code claims.<player uuid>.<service>: <epoch millis>}.
 */
public final class VoteClaimStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Long> claims = new HashMap<>();
    /** False when claims.yml exists but could not be read; claims are then refused. */
    private boolean available = true;

    public VoteClaimStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "claims.yml");
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
        if (root != null) {
            for (String key : root.getKeys(true)) {
                if (!root.isConfigurationSection(key)) {
                    claims.put(key, root.getLong(key));
                }
            }
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /** Epoch millis of the player's claim for a service, or null when unclaimed. */
    public Long claimedAt(UUID playerId, String service) {
        return claims.get(key(playerId, service));
    }

    /** Saves a new claim. Returns false, keeping nothing, if unavailable, already claimed or not saved. */
    public boolean claim(UUID playerId, String service, long claimedAt) {
        String key = key(playerId, service);
        if (!available || claims.containsKey(key)) {
            return false;
        }
        claims.put(key, claimedAt);
        try {
            save();
            return true;
        } catch (IOException failure) {
            claims.remove(key);
            plugin.getLogger().log(Level.SEVERE, "Could not save claims.yml; vote reward not dispensed", failure);
            return false;
        }
    }

    private void save() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        claims.forEach((key, at) -> yaml.set("claims." + key, at));
        File temp = new File(file.getPath() + ".tmp");
        yaml.save(temp);
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static String key(UUID playerId, String service) {
        return playerId + "." + service;
    }
}

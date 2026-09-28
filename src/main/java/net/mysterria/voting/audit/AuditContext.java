package net.mysterria.voting.audit;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;

/** Main-thread helpers that copy Bukkit state into plain audit metadata values. */
public final class AuditContext {
    private static final NamespacedKey ITEM_UUID = NamespacedKey.fromString("circleofimagination:item_uuid");
    private static final NamespacedKey ITEM_PARENT = NamespacedKey.fromString("circleofimagination:item_parent");

    private AuditContext() {
    }

    /** UUID of a player or entity sender; null for console and other non-entity senders. */
    public static UUID actorId(CommandSender sender) {
        return sender instanceof Entity entity ? entity.getUniqueId() : null;
    }

    /** Adds {@code actor_type}, and {@code actor_name} for non-entity senders such as the console. */
    public static void putActor(Map<String, Object> metadata, CommandSender sender) {
        if (sender instanceof Entity) {
            metadata.put("actor_type", "player");
        } else {
            metadata.put("actor_type", "console");
            metadata.put("actor_name", sender.getName());
        }
    }

    /** Adds {@code world}, {@code x}, {@code y}, {@code z} for an entity sender. */
    public static void putLocation(Map<String, Object> metadata, CommandSender sender) {
        if (!(sender instanceof Entity entity)) {
            return;
        }
        Location location = entity.getLocation();
        if (location.getWorld() != null) {
            metadata.put("world", location.getWorld().getName());
        }
        metadata.put("x", location.getBlockX());
        metadata.put("y", location.getBlockY());
        metadata.put("z", location.getBlockZ());
    }

    /** Adds {@code item_material} plus {@code item_uuid} / {@code parent_item_uuid} when tagged. */
    public static void putItem(Map<String, Object> metadata, ItemStack item) {
        if (item == null) {
            return;
        }
        metadata.put("item_material", item.getType().name());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        putTag(metadata, "item_uuid", data, ITEM_UUID);
        putTag(metadata, "parent_item_uuid", data, ITEM_PARENT);
    }

    private static void putTag(Map<String, Object> metadata, String key, PersistentDataContainer data,
                               NamespacedKey tag) {
        if (tag != null && data.has(tag, PersistentDataType.STRING)) {
            String value = data.get(tag, PersistentDataType.STRING);
            if (value != null && !value.isBlank()) {
                metadata.put(key, value);
            }
        }
    }
}

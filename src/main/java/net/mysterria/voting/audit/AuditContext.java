package net.mysterria.voting.audit;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

/** Main-thread helpers that copy Bukkit state into plain audit metadata values. */
public final class AuditContext {
    private AuditContext() {
    }

    public static UUID actorId(CommandSender sender) {
        return sender instanceof Entity entity ? entity.getUniqueId() : null;
    }

    public static void putActor(Map<String, Object> metadata, CommandSender sender) {
        metadata.put("actor_type", sender instanceof Entity ? "player" : "console");
        metadata.put("actor_name", sender.getName());
    }

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

    public static void putItem(Map<String, Object> metadata, ItemStack item) {
        if (item == null) {
            return;
        }
        metadata.put("item_material", item.getType().name());
    }
}

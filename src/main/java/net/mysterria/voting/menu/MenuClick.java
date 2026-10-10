package net.mysterria.voting.menu;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** {@code service} is the menu-items key (for example {@code vote-site}) and the claim scope. */
public record MenuClick(Player player, FileConfiguration langConfig, String lang, String service,
                        int slot, String clickType, ItemStack item) {

    public String actionsPath() {
        return "menu-items." + service + ".click-actions." + clickType;
    }
}

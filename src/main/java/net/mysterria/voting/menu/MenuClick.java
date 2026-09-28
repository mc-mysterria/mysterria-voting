package net.mysterria.voting.menu;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A vote-menu click that matched a configured menu item, captured on the main thread.
 *
 * @param service     the menu-items key (for example {@code vote-site}); the claim scope
 * @param clickType   {@code left} or {@code right}, selecting the click-actions branch
 * @param clickedSide {@code top}, {@code bottom} or {@code outside}
 * @param holderType  simple class name of the top inventory holder, or {@code none}
 */
public record MenuClick(Player player, FileConfiguration langConfig, String lang, String service,
                        int slot, int rawSlot, String clickType, String clickedSide, String holderType,
                        String viewTitle, ItemStack item) {

    public String actionsPath() {
        return "menu-items." + service + ".click-actions." + clickType;
    }
}

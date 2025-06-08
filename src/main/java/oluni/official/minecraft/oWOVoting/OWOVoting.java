package oluni.official.minecraft.oWOVoting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import oluni.official.minecraft.oWOVoting.commands.OWOVotingCMD;
import oluni.official.minecraft.oWOVoting.commands.VotingOpenGui;
import oluni.official.minecraft.oWOVoting.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OWOVoting extends JavaPlugin implements Listener {
    private Map<String, Inventory> cachedMenus = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Objects.requireNonNull(getCommand("voting")).setExecutor(new OWOVotingCMD(this));
        Objects.requireNonNull(getCommand("voting")).setTabCompleter(new OWOVotingCMD(this));
        Objects.requireNonNull(getCommand("vote")).setExecutor(new VotingOpenGui(this));
        Bukkit.getPluginManager().registerEvents(this, this);
        loadMenus();
    }

    @Override
    public void onDisable() {
        cachedMenus.clear();
    }

    public void reload() {
        reloadConfig();
        cachedMenus.clear();
        loadMenus();
    }

    private void loadMenus() {
        Component menuName = MessageUtils.formatMessage(getConfig().getString("menu-name"), null);
        int menuSize = getConfig().getInt("menu-size");
        Inventory inv = Bukkit.createInventory(null, menuSize, menuName);
        if (getConfig().getConfigurationSection("menu-items") != null) {
            for (String key : Objects.requireNonNull(getConfig().getConfigurationSection("menu-items")).getKeys(false)) {
                String path = "menu-items." + key;
                Material mat = Material.valueOf(getConfig().getString(path + ".material"));
                Component displayName = MessageUtils.formatMessage(getConfig().getString(path + ".display-name"), null);
                int slot = getConfig().getInt(path + ".slot");
                List<String> loreRaw = getConfig().getStringList(path + ".lore");
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(displayName.decoration(TextDecoration.ITALIC, false));
                if (!loreRaw.isEmpty()) {
                    List<Component> lore = loreRaw.stream()
                            .map(line -> MessageUtils.formatMessage(line, null).decoration(TextDecoration.ITALIC, false))
                            .toList();
                    meta.lore(lore);
                }
                item.setItemMeta(meta);
                if (slot >= 0 && slot < menuSize) {
                    inv.setItem(slot, item);
                }
            }
        }
        cachedMenus.put("voting", inv);
    }

    public void openVotingGui(Player p) {
        if (cachedMenus.containsKey("voting")) {
            p.openInventory(cachedMenus.get("voting"));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Component menuName = MessageUtils.formatMessage(getConfig().getString("menu-name"), null);
        if (!e.getView().title().equals(menuName)) return;
        e.setCancelled(true);
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        int slot = e.getSlot();
        String clickType = e.getClick().isLeftClick() ? "left" : "right";
        if (getConfig().getConfigurationSection("menu-items") != null) {
            for (String key : Objects.requireNonNull(getConfig().getConfigurationSection("menu-items")).getKeys(false)) {
                String path = "menu-items." + key;
                if (getConfig().getInt(path + ".slot") == slot) {
                    executeClickActions(p, path + ".click-actions." + clickType);
                    break;
                }
            }
        }
    }

    private void executeClickActions(Player p, String path) {
        if (!getConfig().contains(path)) return;
        if (getConfig().contains(path + ".run-command.player")) {
            List<String> playerCmds = getConfig().getStringList(path + ".run-command.player");
            for (String cmd : playerCmds) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("target", p.getName());
                String formattedCmd = MessageUtils.formatPlain(cmd, placeholders);
                p.performCommand(formattedCmd);
                p.closeInventory();
            }
        }
        if (getConfig().contains(path + ".run-command.console")) {
            List<String> consoleCmds = getConfig().getStringList(path + ".run-command.console");
            for (String cmd : consoleCmds) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("target", p.getName());
                String formattedCmd = MessageUtils.formatPlain(cmd, placeholders);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
                p.closeInventory();
            }
        }
        if (getConfig().contains(path + ".message")) {
            List<String> msgs = getConfig().getStringList(path + ".message");
            for (String msg : msgs) {
                p.sendMessage(MessageUtils.formatMessage(msg, null));
                p.closeInventory();
            }
        }
    }
}
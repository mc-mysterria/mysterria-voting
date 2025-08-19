package net.mysterria.voting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.mysterria.voting.commands.VotingCommand;
import net.mysterria.voting.commands.VotingOpenGui;
import net.mysterria.voting.utils.MessageUtils;
import net.mysterria.voting.utils.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
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

public final class MysterriaVoting extends JavaPlugin implements Listener {
    private Map<String, Map<String, Inventory>> cachedMenus = new HashMap<>();
    private TranslationManager translationManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        translationManager = new TranslationManager(this);
        MessageUtils.setTranslationManager(translationManager);
        Objects.requireNonNull(getCommand("voting")).setExecutor(new VotingCommand(this));
        Objects.requireNonNull(getCommand("voting")).setTabCompleter(new VotingCommand(this));
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
        translationManager.reload();
        cachedMenus.clear();
        loadMenus();
    }

    private void loadMenus() {
        int menuSize = getConfig().getInt("menu-size");
        String[] languages = {"en", "uk"};
        
        for (String lang : languages) {
            FileConfiguration langConfig = translationManager.translations.get(lang);
            if (langConfig == null) continue;
            
            Component menuName = MessageUtils.formatMessage(langConfig.getString("menu-name"), null);
            Inventory inv = Bukkit.createInventory(null, menuSize, menuName);
            
            if (langConfig.getConfigurationSection("menu-items") != null) {
                for (String key : Objects.requireNonNull(langConfig.getConfigurationSection("menu-items")).getKeys(false)) {
                    String path = "menu-items." + key;
                    Material mat = Material.valueOf(langConfig.getString(path + ".material"));
                    Component displayName = MessageUtils.formatMessage(langConfig.getString(path + ".display-name"), null);
                    int slot = langConfig.getInt(path + ".slot");
                    List<String> loreRaw = langConfig.getStringList(path + ".lore");
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
            cachedMenus.computeIfAbsent(lang, k -> new HashMap<>()).put("voting", inv);
        }
    }

    public void openVotingGui(Player p) {
        String playerLocale = getPlayerLocale(p);
        Map<String, Inventory> playerMenus = cachedMenus.get(playerLocale);
        if (playerMenus != null && playerMenus.containsKey("voting")) {
            p.openInventory(playerMenus.get("voting"));
        }
    }
    
    private String getPlayerLocale(Player player) {
        String locale = player.locale().getLanguage();
        if (cachedMenus.containsKey(locale)) {
            return locale;
        }
        return "en";
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        
        String playerLocale = getPlayerLocale(p);
        FileConfiguration langConfig = translationManager.translations.get(playerLocale);
        if (langConfig == null) langConfig = translationManager.translations.get("en");
        
        Component menuName = MessageUtils.formatMessage(langConfig.getString("menu-name"), null);
        if (!e.getView().title().equals(menuName)) return;
        e.setCancelled(true);
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        int slot = e.getSlot();
        String clickType = e.getClick().isLeftClick() ? "left" : "right";
        if (langConfig.getConfigurationSection("menu-items") != null) {
            for (String key : Objects.requireNonNull(langConfig.getConfigurationSection("menu-items")).getKeys(false)) {
                String path = "menu-items." + key;
                if (langConfig.getInt(path + ".slot") == slot) {
                    executeClickActions(p, langConfig, path + ".click-actions." + clickType);
                    break;
                }
            }
        }
    }

    private void executeClickActions(Player p, FileConfiguration langConfig, String path) {
        if (!langConfig.contains(path)) return;
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("target", p.getName());
        
        if (langConfig.contains(path + ".run-command.player")) {
            List<String> playerCmds = langConfig.getStringList(path + ".run-command.player");
            for (String cmd : playerCmds) {
                String formattedCmd = MessageUtils.formatPlain(cmd, placeholders);
                p.performCommand(formattedCmd);
            }
        }
        if (langConfig.contains(path + ".run-command.console")) {
            List<String> consoleCmds = langConfig.getStringList(path + ".run-command.console");
            for (String cmd : consoleCmds) {
                String formattedCmd = MessageUtils.formatPlain(cmd, placeholders);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
            }
        }
        if (langConfig.contains(path + ".message")) {
            List<String> msgs = langConfig.getStringList(path + ".message");
            for (String msg : msgs) {
                p.sendMessage(MessageUtils.formatMessage(msg, placeholders));
            }
        }
        if (langConfig.contains(path + ".title")) {
            String titleText = langConfig.getString(path + ".title.title");
            String subtitleText = langConfig.getString(path + ".title.subtitle");
            if (titleText != null || subtitleText != null) {
                MessageUtils.sendTitle(p, titleText, subtitleText, placeholders);
            }
        }
        p.closeInventory();
    }
}
package net.mysterria.voting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.mysterria.voting.claims.VoteClaimStore;
import net.mysterria.voting.commands.ReminderCommand;
import net.mysterria.voting.commands.VotingCommand;
import net.mysterria.voting.commands.VotingOpenGui;
import net.mysterria.voting.reminders.ReminderManager;
import net.mysterria.voting.utils.MessageUtils;
import net.mysterria.voting.utils.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

public final class MysterriaVoting extends JavaPlugin implements Listener {
    
    private Map<String, Map<String, Inventory>> cachedMenus = new HashMap<>();
    /** Menus replaced by a reload that may still be open; clicks in them are cancelled but run nothing. */
    private final Set<Inventory> retiredMenus = Collections.newSetFromMap(new WeakHashMap<>());
    private TranslationManager translationManager;
    private ReminderManager reminderManager;
    private VoteClaimStore claims;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        claims = new VoteClaimStore(this);
        translationManager = new TranslationManager(this);
        MessageUtils.setTranslationManager(translationManager);
        reminderManager = new ReminderManager(this);

        Objects.requireNonNull(getCommand("voting")).setExecutor(new VotingCommand(this));
        Objects.requireNonNull(getCommand("voting")).setTabCompleter(new VotingCommand(this));
        Objects.requireNonNull(getCommand("vote")).setExecutor(new VotingOpenGui(this));
        Objects.requireNonNull(getCommand("reminder")).setExecutor(new ReminderCommand(this));
        Objects.requireNonNull(getCommand("reminder")).setTabCompleter(new ReminderCommand(this));

        Bukkit.getPluginManager().registerEvents(this, this);
        loadMenus();
    }

    @Override
    public void onDisable() {
        if (reminderManager != null) {
            reminderManager.stopAllTasks();
        }
        cachedMenus.clear();
    }

    public void reload() {
        retireMenus();
        cachedMenus.clear();
        reloadConfig();
        translationManager.reload();
        if (reminderManager != null) {
            reminderManager.reload();
        }
        loadMenus();
    }

    private void retireMenus() {
        for (Map<String, Inventory> menus : cachedMenus.values()) {
            retiredMenus.addAll(menus.values());
        }
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

        Inventory top = e.getView().getTopInventory();
        String menuLang = menuLanguageOf(top);
        if (menuLang == null) {
            if (retiredMenus.contains(top)) e.setCancelled(true);
            return;
        }
        e.setCancelled(true);
        // Clicks in the player's own inventory never run actions.
        if (!top.equals(e.getClickedInventory())) return;
        FileConfiguration langConfig = translationManager.translations.get(menuLang);
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        String key = menuItemAt(langConfig, e.getSlot());
        if (key == null) return;
        String clickType = e.getClick().isLeftClick() ? "left" : "right";
        executeClickActions(p, langConfig, key, "menu-items." + key + ".click-actions." + clickType);
    }

    private String menuLanguageOf(Inventory inventory) {
        for (Map.Entry<String, Map<String, Inventory>> entry : cachedMenus.entrySet()) {
            if (inventory.equals(entry.getValue().get("voting"))) return entry.getKey();
        }
        return null;
    }

    private static String menuItemAt(FileConfiguration langConfig, int slot) {
        var items = langConfig.getConfigurationSection("menu-items");
        if (items == null) return null;
        for (String key : items.getKeys(false)) {
            if (items.getInt(key + ".slot") == slot) return key;
        }
        return null;
    }

    private void executeClickActions(Player p, FileConfiguration langConfig, String service, String path) {
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
            // The claim is saved before the reward is dispatched, so a reward is never paid twice.
            if (!consoleCmds.isEmpty() && claims.claim(p.getUniqueId(), service, System.currentTimeMillis())) {
                for (String cmd : consoleCmds) {
                    String formattedCmd = MessageUtils.formatPlain(cmd, placeholders);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
                }
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up any active boss bars for the disconnecting player
        if (reminderManager != null) {
            reminderManager.cleanupPlayerBossBars(event.getPlayer());
        }
    }

    public ReminderManager getReminderManager() {
        return reminderManager;
    }
}
package net.mysterria.voting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.mysterria.voting.audit.AdminAudit;
import net.mysterria.voting.audit.VotingAuditEmitter;
import net.mysterria.voting.claims.VoteClaimStore;
import net.mysterria.voting.commands.ReminderCommand;
import net.mysterria.voting.commands.VotingCommand;
import net.mysterria.voting.commands.VotingOpenGui;
import net.mysterria.voting.menu.MenuClick;
import net.mysterria.voting.menu.VoteClickActions;
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
    private VotingAuditEmitter auditEmitter;
    private AdminAudit adminAudit;
    private VoteClickActions clickActions;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        auditEmitter = new VotingAuditEmitter(this);
        adminAudit = new AdminAudit(this::getAuditEmitter);
        clickActions = new VoteClickActions(new VoteClaimStore(this), this::getAuditEmitter);
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
        try {
            if (reminderManager != null) {
                reminderManager.stopAllTasks();
            }
            cachedMenus.clear();
        } finally {
            if (auditEmitter != null) {
                auditEmitter.close();
                auditEmitter = null;
            }
        }
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
        int slot = e.getSlot();
        String key = menuItemAt(langConfig, slot);
        if (key == null) return;
        String clickType = e.getClick().isLeftClick() ? "left" : "right";
        clickActions.execute(new MenuClick(p, langConfig, menuLang, key, slot, clickType, clickedItem));
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

    public VotingAuditEmitter getAuditEmitter() {
        return auditEmitter;
    }

    public AdminAudit getAdminAudit() {
        return adminAudit;
    }

    public Map<String, FileConfiguration> getTranslations() {
        return translationManager.translations;
    }
}
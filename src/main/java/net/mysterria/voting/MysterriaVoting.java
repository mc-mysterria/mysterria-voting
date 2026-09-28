package net.mysterria.voting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.bukkit.inventory.InventoryHolder;
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
        reloadConfig();
        translationManager.reload();
        if (reminderManager != null) {
            reminderManager.reload();
        }
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
                    clickActions.execute(new MenuClick(p, langConfig, playerLocale, key, slot, e.getRawSlot(),
                            clickType, clickedSide(e), holderType(e.getView().getTopInventory()),
                            PlainTextComponentSerializer.plainText().serialize(e.getView().title()), clickedItem));
                    break;
                }
            }
        }
    }

    private static String clickedSide(InventoryClickEvent e) {
        Inventory clicked = e.getClickedInventory();
        if (clicked == null) return "outside";
        return clicked == e.getView().getTopInventory() ? "top" : "bottom";
    }

    private static String holderType(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder(false);
        return holder == null ? "none" : holder.getClass().getSimpleName();
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

    /** Loaded language files keyed by language code; fingerprinted around a reload for the audit row. */
    public Map<String, FileConfiguration> getTranslations() {
        return translationManager.translations;
    }
}
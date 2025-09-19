package net.mysterria.voting.reminders;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.mysterria.voting.MysterriaVoting;
import net.mysterria.voting.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReminderManager {
    private final MysterriaVoting plugin;
    private final Map<String, Map<String, Reminder>> remindersByLanguage;
    private final Map<String, BukkitTask> tasks;
    private final Map<Player, Set<BossBar>> activeBossBars;
    private final Map<String, FileConfiguration> reminderConfigs;

    public ReminderManager(MysterriaVoting plugin) {
        this.plugin = plugin;
        this.remindersByLanguage = new HashMap<>();
        this.tasks = new HashMap<>();
        this.activeBossBars = new ConcurrentHashMap<>();
        this.reminderConfigs = new HashMap<>();
        loadReminders();
    }

    public void loadReminders() {
        stopAllTasks();
        remindersByLanguage.clear();
        reminderConfigs.clear();

        // Load reminder configurations for each language
        String[] languages = {"en", "uk"};

        for (String lang : languages) {
            loadReminderLanguage(lang);
        }

        // Schedule reminders (only need to schedule once per unique reminder)
        Set<String> scheduledReminders = new HashSet<>();
        for (Map<String, Reminder> languageReminders : remindersByLanguage.values()) {
            for (Reminder reminder : languageReminders.values()) {
                if (reminder.isEnabled() && !scheduledReminders.contains(reminder.getId())) {
                    scheduleReminder(reminder);
                    scheduledReminders.add(reminder.getId());
                }
            }
        }

        int totalReminders = remindersByLanguage.values().stream()
            .mapToInt(Map::size)
            .sum();
        plugin.getLogger().info("Loaded " + totalReminders + " reminders across " + remindersByLanguage.size() + " languages");
    }

    private void loadReminderLanguage(String language) {
        File reminderFile = new File(plugin.getDataFolder(), "reminders/" + language + ".yml");

        // Create the directory and default file if it doesn't exist
        if (!reminderFile.exists()) {
            try {
                reminderFile.getParentFile().mkdirs();

                // Copy default file from resources
                InputStream defaultFile = plugin.getResource("reminders/" + language + ".yml");
                if (defaultFile != null) {
                    Files.copy(defaultFile, reminderFile.toPath());
                    defaultFile.close();
                    plugin.getLogger().info("Created default reminder file for language: " + language);
                } else {
                    plugin.getLogger().warning("Default reminder file not found for language: " + language);
                    return;
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create reminder file for language " + language + ": " + e.getMessage());
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(reminderFile);
        reminderConfigs.put(language, config);

        ConfigurationSection remindersSection = config.getConfigurationSection("reminders");
        if (remindersSection == null) {
            plugin.getLogger().warning("No reminders section found in " + language + ".yml!");
            return;
        }

        Map<String, Reminder> languageReminders = new HashMap<>();
        for (String key : remindersSection.getKeys(false)) {
            ConfigurationSection reminderConfig = remindersSection.getConfigurationSection(key);
            if (reminderConfig != null) {
                Reminder reminder = new Reminder(key, reminderConfig);
                languageReminders.put(key, reminder);
            }
        }

        remindersByLanguage.put(language, languageReminders);
        plugin.getLogger().info("Loaded " + languageReminders.size() + " reminders for language: " + language);
    }

    private void scheduleReminder(Reminder reminder) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                sendReminder(reminder);
            }
        }.runTaskTimer(plugin, 20L * reminder.getInterval(), 20L * reminder.getInterval());

        tasks.put(reminder.getId(), task);
    }

    public void sendReminder(Reminder reminder) {
        Collection<? extends Player> players = getTargetPlayers(reminder);

        for (Player player : players) {
            // Get player's localized reminder
            Reminder localizedReminder = getLocalizedReminder(player, reminder.getId());
            if (localizedReminder == null) {
                continue; // Skip if no localized version found
            }

            if (localizedReminder.getPermission() != null && !player.hasPermission(localizedReminder.getPermission())) {
                continue;
            }

            Map<String, String> placeholders = createPlaceholders(player);

            switch (localizedReminder.getType()) {
                case CHAT:
                    sendChatReminder(player, localizedReminder, placeholders);
                    break;
                case TITLE:
                    sendTitleReminder(player, localizedReminder, placeholders);
                    break;
                case ACTIONBAR:
                    sendActionBarReminder(player, localizedReminder, placeholders);
                    break;
                case BOSSBAR:
                    sendBossBarReminder(player, localizedReminder, placeholders);
                    break;
                case COMBINED:
                    sendCombinedReminder(player, localizedReminder, placeholders);
                    break;
            }
        }
    }

    private Reminder getLocalizedReminder(Player player, String reminderId) {
        String playerLocale = getPlayerLocale(player);
        Map<String, Reminder> languageReminders = remindersByLanguage.get(playerLocale);
        if (languageReminders != null && languageReminders.containsKey(reminderId)) {
            return languageReminders.get(reminderId);
        }

        // Fallback to English if player's language is not available
        Map<String, Reminder> englishReminders = remindersByLanguage.get("en");
        if (englishReminders != null) {
            return englishReminders.get(reminderId);
        }

        return null;
    }

    private String getPlayerLocale(Player player) {
        String locale = player.locale().getLanguage();
        if (remindersByLanguage.containsKey(locale)) {
            return locale;
        }
        return "en"; // Default to English
    }

    private void sendChatReminder(Player player, Reminder reminder, Map<String, String> placeholders) {
        for (String message : reminder.getMessages()) {
            Component formattedMessage = formatReminderMessage(message, reminder, placeholders);
            player.sendMessage(formattedMessage);
        }
    }

    private void sendTitleReminder(Player player, Reminder reminder, Map<String, String> placeholders) {
        String titleText = reminder.getTitleText();
        String subtitleText = reminder.getSubtitleText();

        if (titleText != null || subtitleText != null) {
            MessageUtils.sendTitle(player, titleText, subtitleText, placeholders);
        }
    }

    private void sendActionBarReminder(Player player, Reminder reminder, Map<String, String> placeholders) {
        if (reminder.getActionbarText() != null) {
            Component actionBarMessage = formatReminderMessage(reminder.getActionbarText(), reminder, placeholders);
            player.sendActionBar(actionBarMessage);
        }
    }

    private void sendBossBarReminder(Player player, Reminder reminder, Map<String, String> placeholders) {
        if (reminder.getBossbarText() != null) {
            Component bossBarMessage = formatReminderMessage(reminder.getBossbarText(), reminder, placeholders);

            BossBar bossBar = BossBar.bossBar(
                bossBarMessage,
                1.0f,
                reminder.getBossBarColor(),
                reminder.getBossBarStyle()
            );

            player.showBossBar(bossBar);

            activeBossBars.computeIfAbsent(player, k -> new HashSet<>()).add(bossBar);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.hideBossBar(bossBar);
                Set<BossBar> playerBars = activeBossBars.get(player);
                if (playerBars != null) {
                    playerBars.remove(bossBar);
                    if (playerBars.isEmpty()) {
                        activeBossBars.remove(player);
                    }
                }
            }, 20L * reminder.getBossbarDuration());
        }
    }

    private void sendCombinedReminder(Player player, Reminder reminder, Map<String, String> placeholders) {
        if (!reminder.getMessages().isEmpty()) {
            sendChatReminder(player, reminder, placeholders);
        }
        if (reminder.getTitleText() != null || reminder.getSubtitleText() != null) {
            sendTitleReminder(player, reminder, placeholders);
        }
        if (reminder.getActionbarText() != null) {
            sendActionBarReminder(player, reminder, placeholders);
        }
        if (reminder.getBossbarText() != null) {
            sendBossBarReminder(player, reminder, placeholders);
        }
    }

    private Component formatReminderMessage(String message, Reminder reminder, Map<String, String> placeholders) {
        if (reminder.getGradientConfig() != null) {
            message = applyGradient(message, reminder.getGradientConfig());
        }

        if (reminder.getUrl() != null) {
            String hoverText = reminder.getHoverText() != null ? reminder.getHoverText() : "Click to open!";
            message = "<click:open_url:" + reminder.getUrl() + "><hover:show_text:\"" + hoverText + "\">" + message + "</hover></click>";
        }

        return MessageUtils.formatMessage(message, placeholders);
    }

    private String applyGradient(String message, Map<String, Object> gradientConfig) {
        String startColor = (String) gradientConfig.get("start");
        String endColor = (String) gradientConfig.get("end");

        if (startColor != null && endColor != null) {
            return "<gradient:" + startColor + ":" + endColor + ">" + message + "</gradient>";
        }

        return message;
    }

    private Collection<? extends Player> getTargetPlayers(Reminder reminder) {
        if (reminder.getTargetWorlds().isEmpty()) {
            return Bukkit.getOnlinePlayers();
        }

        List<Player> targetPlayers = new ArrayList<>();
        for (String worldName : reminder.getTargetWorlds()) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                targetPlayers.addAll(world.getPlayers());
            }
        }

        return targetPlayers;
    }

    private Map<String, String> createPlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("world", player.getWorld().getName());
        placeholders.put("displayname", player.getDisplayName());
        placeholders.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        return placeholders;
    }

    public void sendReminderById(String id) {
        // Get any version of the reminder to use as base for scheduling
        Reminder baseReminder = getAnyReminderById(id);
        if (baseReminder != null) {
            sendReminder(baseReminder);
        }
    }

    public void sendReminderToPlayer(String id, Player player) {
        Reminder reminder = getLocalizedReminder(player, id);
        if (reminder != null) {
            if (reminder.getPermission() != null && !player.hasPermission(reminder.getPermission())) {
                return;
            }

            Map<String, String> placeholders = createPlaceholders(player);

            switch (reminder.getType()) {
                case CHAT:
                    sendChatReminder(player, reminder, placeholders);
                    break;
                case TITLE:
                    sendTitleReminder(player, reminder, placeholders);
                    break;
                case ACTIONBAR:
                    sendActionBarReminder(player, reminder, placeholders);
                    break;
                case BOSSBAR:
                    sendBossBarReminder(player, reminder, placeholders);
                    break;
                case COMBINED:
                    sendCombinedReminder(player, reminder, placeholders);
                    break;
            }
        }
    }

    private Reminder getAnyReminderById(String id) {
        for (Map<String, Reminder> languageReminders : remindersByLanguage.values()) {
            if (languageReminders.containsKey(id)) {
                return languageReminders.get(id);
            }
        }
        return null;
    }

    public void stopAllTasks() {
        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();

        for (Map.Entry<Player, Set<BossBar>> entry : activeBossBars.entrySet()) {
            Player player = entry.getKey();
            for (BossBar bossBar : entry.getValue()) {
                player.hideBossBar(bossBar);
            }
        }
        activeBossBars.clear();
    }

    public Map<String, Reminder> getReminders() {
        // Return all reminders from English as base collection
        Map<String, Reminder> englishReminders = remindersByLanguage.get("en");
        return englishReminders != null ? new HashMap<>(englishReminders) : new HashMap<>();
    }

    public Map<String, Reminder> getReminders(String language) {
        Map<String, Reminder> languageReminders = remindersByLanguage.get(language);
        return languageReminders != null ? new HashMap<>(languageReminders) : new HashMap<>();
    }

    public Reminder getReminder(String id) {
        return getAnyReminderById(id);
    }

    public Reminder getReminder(String id, String language) {
        Map<String, Reminder> languageReminders = remindersByLanguage.get(language);
        if (languageReminders != null) {
            return languageReminders.get(id);
        }
        return null;
    }

    public void reload() {
        stopAllTasks();
        loadReminders();
    }

    public void cleanupPlayerBossBars(Player player) {
        Set<BossBar> playerBars = activeBossBars.remove(player);
        if (playerBars != null) {
            for (BossBar bossBar : playerBars) {
                player.hideBossBar(bossBar);
            }
        }
    }
}
package net.mysterria.voting.reminders;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.mysterria.voting.MysterriaVoting;
import net.mysterria.voting.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReminderManager {
    private final MysterriaVoting plugin;
    private final Map<String, Reminder> reminders;
    private final Map<String, BukkitTask> tasks;
    private final Map<Player, Set<BossBar>> activeBossBars;

    public ReminderManager(MysterriaVoting plugin) {
        this.plugin = plugin;
        this.reminders = new HashMap<>();
        this.tasks = new HashMap<>();
        this.activeBossBars = new ConcurrentHashMap<>();
        loadReminders();
    }

    public void loadReminders() {
        stopAllTasks();
        reminders.clear();

        ConfigurationSection remindersSection = plugin.getConfig().getConfigurationSection("reminders");
        if (remindersSection == null) {
            plugin.getLogger().warning("No reminders section found in config!");
            return;
        }

        for (String key : remindersSection.getKeys(false)) {
            ConfigurationSection reminderConfig = remindersSection.getConfigurationSection(key);
            if (reminderConfig != null) {
                Reminder reminder = new Reminder(key, reminderConfig);
                reminders.put(key, reminder);

                if (reminder.isEnabled()) {
                    scheduleReminder(reminder);
                }
            }
        }

        plugin.getLogger().info("Loaded " + reminders.size() + " reminders");
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
            if (reminder.getPermission() != null && !player.hasPermission(reminder.getPermission())) {
                continue;
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
        Reminder reminder = reminders.get(id);
        if (reminder != null) {
            sendReminder(reminder);
        }
    }

    public void sendReminderToPlayer(String id, Player player) {
        Reminder reminder = reminders.get(id);
        if (reminder != null) {
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
        return new HashMap<>(reminders);
    }

    public Reminder getReminder(String id) {
        return reminders.get(id);
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
package net.mysterria.voting.reminders;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public class Reminder {
    private final String id;
    private final String name;
    private final ReminderType type;
    private final List<String> messages;
    private final String titleText;
    private final String subtitleText;
    private final String actionbarText;
    private final String bossbarText;
    private final String bossbarColor;
    private final String bossbarStyle;
    private final int bossbarDuration;
    private final String url;
    private final String hoverText;
    private final boolean enabled;
    private final int interval; // in seconds
    private final List<String> targetWorlds;
    private final String permission;
    private final Map<String, Object> gradientConfig;

    public Reminder(String id, ConfigurationSection config) {
        this.id = id;
        this.name = config.getString("name", id);
        this.type = ReminderType.valueOf(config.getString("type", "CHAT").toUpperCase());
        this.messages = config.getStringList("messages");
        this.titleText = config.getString("title.text");
        this.subtitleText = config.getString("title.subtitle");
        this.actionbarText = config.getString("actionbar.text");
        this.bossbarText = config.getString("bossbar.text");
        this.bossbarColor = config.getString("bossbar.color", "BLUE");
        this.bossbarStyle = config.getString("bossbar.style", "SOLID");
        this.bossbarDuration = config.getInt("bossbar.duration", 10);
        this.url = config.getString("url");
        this.hoverText = config.getString("hover-text");
        this.enabled = config.getBoolean("enabled", true);
        this.interval = config.getInt("interval", 300); // 5 minutes default
        this.targetWorlds = config.getStringList("target-worlds");
        this.permission = config.getString("permission");
        this.gradientConfig = config.getConfigurationSection("gradient") != null ?
            config.getConfigurationSection("gradient").getValues(false) : null;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public ReminderType getType() { return type; }
    public List<String> getMessages() { return messages; }
    public String getTitleText() { return titleText; }
    public String getSubtitleText() { return subtitleText; }
    public String getActionbarText() { return actionbarText; }
    public String getBossbarText() { return bossbarText; }
    public String getBossbarColor() { return bossbarColor; }
    public String getBossbarStyle() { return bossbarStyle; }
    public int getBossbarDuration() { return bossbarDuration; }
    public String getUrl() { return url; }
    public String getHoverText() { return hoverText; }
    public boolean isEnabled() { return enabled; }
    public int getInterval() { return interval; }
    public List<String> getTargetWorlds() { return targetWorlds; }
    public String getPermission() { return permission; }
    public Map<String, Object> getGradientConfig() { return gradientConfig; }

    public BossBar.Color getBossBarColor() {
        try {
            return BossBar.Color.valueOf(bossbarColor.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.BLUE;
        }
    }

    public BossBar.Overlay getBossBarStyle() {
        try {
            return BossBar.Overlay.valueOf(bossbarStyle.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}
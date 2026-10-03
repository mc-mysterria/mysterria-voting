package net.mysterria.voting.audit;

import net.mysterria.voting.reminders.Reminder;
import net.mysterria.voting.reminders.ReminderManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** {@code menuHash} is null when menus are out of scope (reminder-only reload). */
public record ConfigFingerprint(String menuHash, String consoleTemplates, int consoleTemplateCount,
                                String remindersHash, int reminderCount) {
    private static final String[] LANGUAGES = {"en", "uk"};

    public static ConfigFingerprint reminders(ReminderManager reminders) {
        StringBuilder canonical = new StringBuilder();
        int count = appendReminders(canonical, reminders);
        return new ConfigFingerprint(null, null, 0, hash(canonical), count);
    }

    public static ConfigFingerprint full(Map<String, FileConfiguration> translations, ReminderManager reminders) {
        StringBuilder menus = new StringBuilder();
        TreeSet<String> templates = new TreeSet<>();
        new TreeMap<>(translations).forEach((lang, config) -> appendMenu(menus, templates, lang, config));
        ConfigFingerprint base = reminders(reminders);
        return new ConfigFingerprint(hash(menus), String.join(" | ", templates), templates.size(),
                base.remindersHash(), base.reminderCount());
    }

    private static void appendMenu(StringBuilder out, TreeSet<String> templates, String lang, FileConfiguration config) {
        out.append(lang).append("|menu-name|").append(config.getString("menu-name")).append('\n');
        ConfigurationSection items = config.getConfigurationSection("menu-items");
        if (items == null) return;
        for (String key : new TreeSet<>(items.getKeys(false))) {
            out.append(lang).append('|').append(key).append("|slot|").append(items.getInt(key + ".slot")).append('\n');
            ConfigurationSection actions = items.getConfigurationSection(key + ".click-actions");
            if (actions == null) continue;
            for (String click : new TreeSet<>(actions.getKeys(false))) {
                for (String kind : List.of("console", "player")) {
                    for (String cmd : actions.getStringList(click + ".run-command." + kind)) {
                        out.append(lang).append('|').append(key).append('|').append(click).append('|')
                                .append(kind).append('|').append(cmd).append('\n');
                        if (kind.equals("console")) templates.add(cmd);
                    }
                }
            }
        }
    }

    private static int appendReminders(StringBuilder out, ReminderManager reminders) {
        int count = 0;
        if (reminders == null) return count;
        for (String lang : LANGUAGES) {
            for (Reminder reminder : new TreeMap<>(reminders.getReminders(lang)).values()) {
                count++;
                out.append(lang).append('|').append(reminder.getId()).append('|').append(reminder.getType())
                        .append('|').append(reminder.isEnabled()).append('|').append(reminder.getInterval())
                        .append('|').append(reminder.getPermission()).append('|').append(reminder.getUrl())
                        .append('|').append(reminder.getMessages()).append('\n');
            }
        }
        return count;
    }

    private static String hash(CharSequence canonical) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException unavailable) {
            return "unavailable";
        }
    }
}

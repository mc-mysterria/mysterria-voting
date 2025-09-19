package net.mysterria.voting.commands;

import net.mysterria.voting.MysterriaVoting;
import net.mysterria.voting.reminders.Reminder;
import net.mysterria.voting.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ReminderCommand implements CommandExecutor, TabCompleter {
    private final MysterriaVoting plugin;

    public ReminderCommand(MysterriaVoting plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                handleListCommand(sender);
                break;
            case "send":
                handleSendCommand(sender, args);
                break;
            case "sendto":
                handleSendToCommand(sender, args);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "info":
                handleInfoCommand(sender, args);
                break;
            case "help":
                sendHelpMessage(sender);
                break;
            default:
                sender.sendMessage(MessageUtils.formatMessage("<red>Unknown subcommand. Use /reminder help for available commands.", null));
                break;
        }

        return true;
    }

    private void handleListCommand(CommandSender sender) {
        if (!sender.hasPermission("voting.reminder.list")) {
            sender.sendMessage(MessageUtils.formatMessage("<red>You don't have permission for this command!", null));
            return;
        }

        Map<String, Reminder> reminders = plugin.getReminderManager().getReminders();
        if (reminders.isEmpty()) {
            sender.sendMessage(MessageUtils.formatMessage("<yellow>No reminders configured.", null));
            return;
        }

        sender.sendMessage(MessageUtils.formatMessage("<green>Available reminders (English base):", null));
        for (Reminder reminder : reminders.values()) {
            String status = reminder.isEnabled() ? "<green>✓" : "<red>✗";
            sender.sendMessage(MessageUtils.formatMessage(
                "<gray>- <white>" + reminder.getId() + " " + status + " <gray>(" + reminder.getType() + ", " + reminder.getInterval() + "s)",
                null
            ));
        }

        // Show language availability
        sender.sendMessage(MessageUtils.formatMessage("<yellow>Available languages: en, uk", null));
        sender.sendMessage(MessageUtils.formatMessage("<gray>Use /reminder info <id> for detailed information", null));
    }

    private void handleSendCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("voting.reminder.send")) {
            sender.sendMessage(MessageUtils.formatMessage("<red>You don't have permission for this command!", null));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.formatMessage("<red>Usage: /reminder send <reminder_id>", null));
            return;
        }

        String reminderId = args[1];
        Reminder reminder = plugin.getReminderManager().getReminder(reminderId);

        if (reminder == null) {
            sender.sendMessage(MessageUtils.formatMessage("<red>Reminder '" + reminderId + "' not found!", null));
            return;
        }

        plugin.getReminderManager().sendReminderById(reminderId);
        sender.sendMessage(MessageUtils.formatMessage("<green>Reminder '" + reminderId + "' sent to all players!", null));
    }

    private void handleSendToCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("voting.reminder.sendto")) {
            sender.sendMessage(MessageUtils.formatMessage("<red>You don't have permission for this command!", null));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatMessage("<red>Usage: /reminder sendto <player> <reminder_id>", null));
            return;
        }

        String playerName = args[1];
        String reminderId = args[2];

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(MessageUtils.formatMessage("<red>Player '" + playerName + "' not found!", null));
            return;
        }

        Reminder reminder = plugin.getReminderManager().getReminder(reminderId);
        if (reminder == null) {
            sender.sendMessage(MessageUtils.formatMessage("<red>Reminder '" + reminderId + "' not found!", null));
            return;
        }

        plugin.getReminderManager().sendReminderToPlayer(reminderId, target);
        sender.sendMessage(MessageUtils.formatMessage("<green>Reminder '" + reminderId + "' sent to " + target.getName() + "!", null));
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("voting.reminder.reload")) {
            sender.sendMessage(MessageUtils.formatMessage("<red>You don't have permission for this command!", null));
            return;
        }

        plugin.getReminderManager().reload();
        sender.sendMessage(MessageUtils.formatMessage("<green>Reminders reloaded successfully!", null));
    }

    private void handleInfoCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("voting.reminder.info")) {
            sender.sendMessage(MessageUtils.formatMessage("<red>You don't have permission for this command!", null));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.formatMessage("<red>Usage: /reminder info <reminder_id>", null));
            return;
        }

        String reminderId = args[1];
        Reminder baseReminder = plugin.getReminderManager().getReminder(reminderId);

        if (baseReminder == null) {
            sender.sendMessage(MessageUtils.formatMessage("<red>Reminder '" + reminderId + "' not found!", null));
            return;
        }

        sender.sendMessage(MessageUtils.formatMessage("<green>Reminder Info: <white>" + baseReminder.getId(), null));
        sender.sendMessage(MessageUtils.formatMessage("<gray>Type: <white>" + baseReminder.getType(), null));
        sender.sendMessage(MessageUtils.formatMessage("<gray>Enabled: <white>" + baseReminder.isEnabled(), null));
        sender.sendMessage(MessageUtils.formatMessage("<gray>Interval: <white>" + baseReminder.getInterval() + "s", null));

        if (baseReminder.getPermission() != null) {
            sender.sendMessage(MessageUtils.formatMessage("<gray>Permission: <white>" + baseReminder.getPermission(), null));
        }

        if (!baseReminder.getTargetWorlds().isEmpty()) {
            sender.sendMessage(MessageUtils.formatMessage("<gray>Worlds: <white>" + String.join(", ", baseReminder.getTargetWorlds()), null));
        }

        if (baseReminder.getUrl() != null) {
            sender.sendMessage(MessageUtils.formatMessage("<gray>URL: <white>" + baseReminder.getUrl(), null));
        }

        // Show language-specific information
        sender.sendMessage(MessageUtils.formatMessage("<yellow>Language versions:", null));
        String[] languages = {"en", "uk"};
        for (String lang : languages) {
            Reminder langReminder = plugin.getReminderManager().getReminder(reminderId, lang);
            if (langReminder != null) {
                sender.sendMessage(MessageUtils.formatMessage(
                    "<gray>  " + lang.toUpperCase() + ": <white>" + langReminder.getName(), null
                ));
            }
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(MessageUtils.formatMessage("<green>Reminder Commands:", null));
        sender.sendMessage(MessageUtils.formatMessage("<gray>/reminder list <white>- List all reminders", null));
        sender.sendMessage(MessageUtils.formatMessage("<gray>/reminder send <reminder_id> <white>- Send reminder to all players", null));
        sender.sendMessage(MessageUtils.formatMessage("<gray>/reminder sendto <player> <reminder_id> <white>- Send reminder to specific player", null));
        sender.sendMessage(MessageUtils.formatMessage("<gray>/reminder info <reminder_id> <white>- Show reminder information", null));
        sender.sendMessage(MessageUtils.formatMessage("<gray>/reminder reload <white>- Reload reminders", null));
        sender.sendMessage(MessageUtils.formatMessage("<gray>/reminder help <white>- Show this help", null));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("list", "send", "sendto", "reload", "info", "help");
            return subCommands.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if ("send".equals(subCommand) || "info".equals(subCommand)) {
                return plugin.getReminderManager().getReminders().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            } else if ("sendto".equals(subCommand)) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && "sendto".equals(args[0].toLowerCase())) {
            return plugin.getReminderManager().getReminders().keySet().stream()
                .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
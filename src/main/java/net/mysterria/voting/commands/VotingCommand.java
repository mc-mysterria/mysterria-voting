package net.mysterria.voting.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.mysterria.voting.MysterriaVoting;
import net.mysterria.voting.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VotingCommand implements CommandExecutor, TabCompleter {

    private final MysterriaVoting plugin;

    public VotingCommand(MysterriaVoting plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String @NotNull [] a) {
        if (a.length == 0) {
            if (s instanceof Player p) {
                s.sendMessage(MessageUtils.formatTranslatedMessage(p, "msg.invalid-usage", null));
            } else {
                s.sendMessage(MessageUtils.formatMessage("Usage: /voting [send|reload]", null));
            }
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "reload":
                if (!s.hasPermission("voting.reload")) {
                    if (s instanceof Player p) {
                        s.sendMessage(MessageUtils.formatTranslatedMessage(p, "msg.no-permission", null));
                    } else {
                        s.sendMessage(MessageUtils.formatMessage("You don't have permission for this command!", null));
                    }
                    return true;
                }
                plugin.reload();
                if (s instanceof Player p) {
                    s.sendMessage(MessageUtils.formatTranslatedMessage(p, "msg.reload-success", null));
                } else {
                    s.sendMessage(MessageUtils.formatMessage("Configuration reloaded successfully!", null));
                }
                return true;

            case "send":
                if (!s.hasPermission("voting.send")) {
                    if (s instanceof Player p) {
                        s.sendMessage(MessageUtils.formatTranslatedMessage(p, "msg.no-permission", null));
                    } else {
                        s.sendMessage(MessageUtils.formatMessage("You don't have permission for this command!", null));
                    }
                    return true;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Component msgComponent = MessageUtils.formatTranslatedMessage(p, "msg.voting-message", null);
                    Component hoverComponent = MessageUtils.formatTranslatedMessage(p, "msg.hover-text", null);
                    Component finalComponent = msgComponent
                            .clickEvent(ClickEvent.runCommand("/vote"))
                            .hoverEvent(HoverEvent.showText(hoverComponent));
                    p.sendMessage(finalComponent);
                }
                return true;

            default:
                if (s instanceof Player p) {
                    s.sendMessage(MessageUtils.formatTranslatedMessage(p, "msg.invalid-usage", null));
                } else {
                    s.sendMessage(MessageUtils.formatMessage("Usage: /voting [send|reload]", null));
                }
                return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String st, @NotNull String @NotNull [] a) {
        return List.of("reload", "send");
    }
}
package oluni.official.minecraft.oWOVoting.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import oluni.official.minecraft.oWOVoting.OWOVoting;
import oluni.official.minecraft.oWOVoting.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OWOVotingCMD implements CommandExecutor, TabCompleter {
    private final OWOVoting pl;

    public OWOVotingCMD(OWOVoting pl) {
        this.pl = pl;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String @NotNull [] a) {
        if (a.length == 0) {
            s.sendMessage(MessageUtils.formatMessage(pl.getConfig().getString("msg.invalid-usage"), null));
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "reload":
                if (!s.hasPermission("owovoting.reload")) {
                    s.sendMessage(MessageUtils.formatMessage(pl.getConfig().getString("msg.no-permission"), null));
                    return true;
                }
                pl.reload();
                s.sendMessage(MessageUtils.formatMessage(pl.getConfig().getString("msg.reload-success"), null));
                return true;

            case "send":
                if (!s.hasPermission("owovoting.send")) {
                    s.sendMessage(MessageUtils.formatMessage(pl.getConfig().getString("msg.no-permission"), null));
                    return true;
                }
                Component msgComponent = MessageUtils.formatMessage(pl.getConfig().getString("msg.voting-message"), null);
                Component hoverComponent = MessageUtils.formatMessage(pl.getConfig().getString("msg.hover-text"), null);
                Component finalComponent = msgComponent
                        .clickEvent(ClickEvent.runCommand("/vote"))
                        .hoverEvent(HoverEvent.showText(hoverComponent));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(finalComponent);
                }
                return true;

            default:
                s.sendMessage(MessageUtils.formatMessage(pl.getConfig().getString("msg.invalid-usage"), null));
                return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String st, @NotNull String @NotNull [] a) {
        return List.of("reload", "send");
    }
}
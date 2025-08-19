package net.mysterria.voting.commands;

import net.mysterria.voting.MysterriaVoting;
import net.mysterria.voting.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VotingOpenGui implements CommandExecutor {

    private final MysterriaVoting plugin;

    public VotingOpenGui(MysterriaVoting plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String @NotNull [] a) {
        if (!(s instanceof Player p)) {
            s.sendMessage(MessageUtils.formatMessage("This command can only be executed by a player!", null));
            return true;
        }
        if (!p.hasPermission("voting.vote")) {
            p.sendMessage(MessageUtils.formatTranslatedMessage(p, "msg.no-permission", null));
            return true;
        }
        plugin.openVotingGui(p);
        return true;
    }
}
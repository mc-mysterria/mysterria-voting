package oluni.official.minecraft.oWOVoting.commands;

import oluni.official.minecraft.oWOVoting.OWOVoting;
import oluni.official.minecraft.oWOVoting.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
public class VotingOpenGui implements CommandExecutor {
    private final OWOVoting pl;

    public VotingOpenGui(OWOVoting pl) {
        this.pl = pl;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String @NotNull [] a) {
        if (!(s instanceof Player p)) {
            s.sendMessage(MessageUtils.formatMessage(pl.getConfig().getString("msg.only-player"), null));
            return true;
        }
        if (!p.hasPermission("owovoting.vote")) {
            p.sendMessage(MessageUtils.formatMessage(pl.getConfig().getString("msg.no-permission"), null));
            return true;
        }
        pl.openVotingGui(p);
        return true;
    }
}
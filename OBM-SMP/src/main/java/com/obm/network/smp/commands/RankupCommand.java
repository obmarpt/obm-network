package com.obm.network.smp.commands;

import com.obm.network.core.ui.PlayerUx;
import com.obm.network.smp.permission.SmpPermissions;
import com.obm.network.smp.progression.RankGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RankupCommand implements CommandExecutor {

    private final RankGui rankGui;

    public RankupCommand(RankGui rankGui) {
        this.rankGui = rankGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!PlayerUx.requirePlayer(sender)) {
            return true;
        }
        Player player = (Player) sender;
        if (SmpPermissions.deny(player, SmpPermissions.RANK, "Precisas de: obm.smp.rank")) {
            return true;
        }

        PlayerUx.openGuiFeedback(player, "Rank SMP");
        rankGui.open(player);
        return true;
    }
}

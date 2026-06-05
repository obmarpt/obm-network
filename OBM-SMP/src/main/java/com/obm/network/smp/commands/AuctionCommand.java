package com.obm.network.smp.commands;

import com.obm.network.core.ui.PlayerUx;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.auction.AuctionGui;
import com.obm.network.smp.permission.SmpPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AuctionCommand implements CommandExecutor {

    private final AuctionGui auctionGui;
    private final WorldModeService worldModeService;

    public AuctionCommand(AuctionGui auctionGui, WorldModeService worldModeService) {
        this.auctionGui = auctionGui;
        this.worldModeService = worldModeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!PlayerUx.requirePlayer(sender)) {
            return true;
        }
        Player player = (Player) sender;

        if (!worldModeService.isSMP(player.getWorld().getName())) {
            PlayerUx.error(player, "Este comando só funciona dentro do SMP.");
            return true;
        }
        if (SmpPermissions.deny(player, SmpPermissions.AUCTION, "Precisas de: obm.smp.auction")) {
            return true;
        }

        PlayerUx.openGuiFeedback(player, "Leilão");
        auctionGui.openMain(player);
        return true;
    }
}

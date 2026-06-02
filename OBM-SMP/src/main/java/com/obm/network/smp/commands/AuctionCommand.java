package com.obm.network.smp.commands;

import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.auction.AuctionGui;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        if (!worldModeService.isSMP(player.getWorld().getName())) {
            player.sendMessage("§cEste comando só funciona dentro do SMP.");
            return true;
        }

        auctionGui.openMain(player);
        return true;
    }
}

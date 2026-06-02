package com.obm.network.smp.commands;

import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.sell.SellGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellCommand implements CommandExecutor {

    private final SellGui sellGui;
    private final WorldModeService worldModeService;

    public SellCommand(SellGui sellGui, WorldModeService worldModeService) {
        this.sellGui = sellGui;
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

        sellGui.open(player);
        return true;
    }
}

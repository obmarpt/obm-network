package com.obm.network.smp.commands;

import com.obm.network.core.ui.PlayerUx;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.permission.SmpPermissions;
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
        if (!PlayerUx.requirePlayer(sender)) {
            return true;
        }
        Player player = (Player) sender;

        if (!worldModeService.isSMP(player.getWorld().getName())) {
            PlayerUx.error(player, "Este comando só funciona dentro do SMP.");
            return true;
        }
        if (SmpPermissions.deny(player, SmpPermissions.SELL, "Precisas de: obm.smp.sell")) {
            return true;
        }

        PlayerUx.openGuiFeedback(player, "Venda de Itens");
        sellGui.open(player);
        return true;
    }
}

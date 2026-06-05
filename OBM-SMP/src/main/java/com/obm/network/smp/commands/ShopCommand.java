package com.obm.network.smp.commands;

import com.obm.network.core.ui.PlayerUx;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.permission.SmpPermissions;
import com.obm.network.smp.shop.ShopGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final ShopGui shopGui;
    private final WorldModeService worldModeService;

    public ShopCommand(ShopGui shopGui, WorldModeService worldModeService) {
        this.shopGui = shopGui;
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
            PlayerUx.errorSound(player);
            return true;
        }
        if (SmpPermissions.deny(player, SmpPermissions.SHOP, "Precisas de: obm.smp.shop")) {
            return true;
        }

        PlayerUx.openGuiFeedback(player, "Loja SMP");
        shopGui.openCategories(player);
        return true;
    }
}

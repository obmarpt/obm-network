package com.obm.network.smp.commands;

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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        if (!worldModeService.isSMP(player.getWorld().getName())) {
            player.sendMessage("§cEste comando só funciona dentro do SMP.");
            return true;
        }
        if (SmpPermissions.deny(player, SmpPermissions.SHOP, "Permissão: obm.smp.shop")) {
            return true;
        }

        SmpPermissions.debug(player, "A abrir loja (/shop)");
        shopGui.openCategories(player);
        return true;
    }
}

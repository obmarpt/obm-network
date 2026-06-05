package com.obm.network.smp.hud;

import com.obm.network.core.ui.PlayerUx;
import com.obm.network.core.world.WorldModeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HudCommand implements CommandExecutor {

    private final HudGui hudGui;
    private final WorldModeService worldModeService;

    public HudCommand(HudGui hudGui, WorldModeService worldModeService) {
        this.hudGui = hudGui;
        this.worldModeService = worldModeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!PlayerUx.requirePlayer(sender)) {
            return true;
        }
        Player player = (Player) sender;
        if (!worldModeService.isSMP(player.getWorld().getName())) {
            PlayerUx.error(player, "O /hud só está disponível no SMP.");
            return true;
        }
        hudGui.open(player);
        PlayerUx.confirmSound(player);
        return true;
    }
}

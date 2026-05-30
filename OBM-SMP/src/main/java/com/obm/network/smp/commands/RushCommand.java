package com.obm.network.smp.commands;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.SMPPlugin;
import com.obm.network.core.combat.CooldownService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RushCommand implements CommandExecutor {

    private final SMPPlugin plugin;

    public RushCommand(SMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        WorldModeService worldService = plugin.getWorldModeService();
        String rushWorld = worldService.getPrimaryRushWorld();
        World world = Bukkit.getWorld(rushWorld);

        if (world == null) {
            player.sendMessage("§cErro: mundo Rush não encontrado. Verifique a configuração em OBM-Core.");
            return true;
        }

        if (world.getName().equalsIgnoreCase(player.getWorld().getName())) {
            player.sendMessage("§aVocê já está no Rush SMP.");
            return true;
        }

        String lockKey = "location_restore_locked_" + player.getUniqueId();
        OBMCorePlugin.get().getDataStore().setBoolean(player.getUniqueId(), lockKey, true);
        OBMCorePlugin.get().getDataStore().save(player.getUniqueId());
        player.teleport(world.getSpawnLocation());
        player.sendMessage("§aTeleportando para Rush SMP: §e" + rushWorld);
        CooldownService.setEnteredMode(player);
        return true;
    }
}

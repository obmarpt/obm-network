package com.obm.network.smp.commands;

import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.SMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SMPCommand implements CommandExecutor {

    private final SMPPlugin plugin;

    public SMPCommand(SMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        WorldModeService worldService = plugin.getWorldModeService();
        String smpWorld = worldService.getPrimarySMPWorld();
        World world = Bukkit.getWorld(smpWorld);

        if (world == null) {
            player.sendMessage("§cErro: mundo SMP não encontrado. Verifique a configuração em OBM-Core.");
            return true;
        }

        if (world.getName().equalsIgnoreCase(player.getWorld().getName())) {
            player.sendMessage("§aVocê já está no SMP.");
            return true;
        }

        player.teleport(world.getSpawnLocation());
        player.sendMessage("§aTeleportando para o SMP: §e" + smpWorld);
        return true;
    }
}

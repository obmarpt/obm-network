package com.obm.network.smp.commands;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.combat.CooldownService;
import com.obm.network.smp.SMPPlugin;
import com.obm.network.smp.permission.SmpPermissions;
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
        if (SmpPermissions.deny(player, SmpPermissions.ENTER, "Permissão: obm.smp.enter")) {
            return true;
        }

        String lockKey = "location_restore_locked_" + player.getUniqueId();
        OBMCorePlugin.get().getDataStore().setBoolean(player.getUniqueId(), lockKey, true);
        OBMCorePlugin.get().getDataStore().save(player.getUniqueId());

        if (plugin.getSMPManager().handleEnter(player)) {
            CooldownService.setEnteredMode(player);
        }
        return true;
    }
}

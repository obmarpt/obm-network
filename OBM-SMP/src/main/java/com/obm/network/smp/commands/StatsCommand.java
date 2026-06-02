package com.obm.network.smp.commands;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StatsCommand implements CommandExecutor {

    private final DataStore dataStore;

    public StatsCommand() {
        this.dataStore = OBMCorePlugin.get().getDataStore();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 3) {
            sender.sendMessage("§cUso: /stats <modo> <stat> <player>");
            return true;
        }

        String mode = args[0].toLowerCase();
        String stat = args[1].toLowerCase();
        String playerName = args[2];

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();

        String key = stat + "_" + mode;

        int value = dataStore.getInt(uuid, key);

        sender.sendMessage("§6Stats de §e" + target.getName());
        sender.sendMessage("§7Modo: §f" + mode);
        sender.sendMessage("§7" + stat + ": §a" + value);

        return true;
    }
}
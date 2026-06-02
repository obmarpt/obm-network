package com.obm.network.tierspace.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

final class SpartanBridge {

    private SpartanBridge() {
    }

    static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("Spartan") != null;
    }

    static void applyTimedBypass(Player player, int seconds) {
        if (!isAvailable() || player == null) {
            return;
        }
        String command = "spartan bypass " + player.getName() + " all " + seconds;
        org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("OBM-TierSpace");
        if (plugin == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }
}

package com.obm.network.tierspace.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class LiteBansBridge {

    private LiteBansBridge() {
    }

    static void tempban(Player player, int days, String reason) {
        if (player == null) {
            return;
        }
        String command = "tempban " + player.getName() + " " + days + "d " + reason;
        org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("OBM-TierSpace");
        if (plugin == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }
}

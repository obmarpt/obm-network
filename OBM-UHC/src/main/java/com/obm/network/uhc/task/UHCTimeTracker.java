package com.obm.network.uhc.task;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.integration.HardcoreStatsBridge;
import com.obm.network.uhc.util.UHCUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class UHCTimeTracker {

    public UHCTimeTracker(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!UHCUtils.isUHC(p)) {
                    continue;
                }
                if (UHCUtils.getLives(p) > 0) {
                    HardcoreStatsBridge.addTimeAlive(p, 1);
                }
            }
        }, 40L, 40L);
    }
}
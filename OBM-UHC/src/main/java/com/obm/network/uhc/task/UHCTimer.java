package com.obm.network.uhc.task;

import com.obm.network.uhc.OBMUHCPlugin;
import com.obm.network.uhc.manager.UHCManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class UHCTimer {

    public UHCTimer(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            UHCManager manager = OBMUHCPlugin.get().getUHCManager();
            if (manager == null) {
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!manager.isUHC(player)) {
                    continue;
                }
                manager.checkProtection(player);
            }
        }, 0L, 1200L);
    }
}

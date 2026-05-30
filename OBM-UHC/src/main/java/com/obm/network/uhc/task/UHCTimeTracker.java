package com.obm.network.uhc.task;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import com.obm.network.uhc.util.UHCUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class UHCTimeTracker {

    private final DataStore ds = OBMCorePlugin.get().getDataStore();

    public UHCTimeTracker(Plugin plugin) {

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            for (Player p : Bukkit.getOnlinePlayers()) {

                if (!UHCUtils.isUHC(p)) continue;

                // ✅ só conta se ainda estiver vivo
                if (UHCUtils.getLives(p) > 0) {

                    ds.increment(p.getUniqueId(), "time_alive_uhc");
                }
            }

        }, 20L, 20L); // a cada 1 segundo
    }
}
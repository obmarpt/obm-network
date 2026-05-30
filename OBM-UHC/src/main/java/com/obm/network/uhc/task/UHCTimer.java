package com.obm.network.uhc.task;
import com.obm.network.uhc.manager.UHCManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class UHCTimer {

    private final UHCManager manager = new UHCManager();

    public UHCTimer(Plugin plugin) {

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            for (Player p : Bukkit.getOnlinePlayers()) {

                if (!manager.isUHC(p)) continue;

                manager.checkProtection(p);
            }

        }, 0L, 1200L); // verifica a cada 1 minuto
    }
}
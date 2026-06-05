package com.obm.network.lobby.gui;

import com.obm.network.lobby.OBMLobbyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Atualiza contagens no navegador de modos sem recriar inventários (a cada ~3s).
 */
public final class ModeNavigatorRefreshTask extends BukkitRunnable {

    private final OBMLobbyPlugin plugin;

    public ModeNavigatorRefreshTask(OBMLobbyPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        runTaskTimer(plugin, 60L, 60L);
    }

    @Override
    public void run() {
        String title = MainMenuConfig.title();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            if (!title.equals(player.getOpenInventory().getTitle())) {
                continue;
            }
            var top = player.getOpenInventory().getTopInventory();
            MainMenu.refreshModeSlots(top, player);
        }
    }
}

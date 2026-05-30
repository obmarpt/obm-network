package com.obm.network.lobby.join;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class QuitHandler implements Listener {

    private final Plugin plugin;
    private final DataStore ds;
    private static final long LOGOUT_TIMEOUT = 60000L; // 1 minuto

    public QuitHandler(Plugin plugin) {
        this.plugin = plugin;
        this.ds = OBMCorePlugin.get().getDataStore();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        // 📍 Guardar a última localização do mundo anterior
        ds.set(player.getUniqueId(), "last_logout_location", player.getLocation());

        // ⏱️ Guardar o tempo de saída
        ds.set(player.getUniqueId(), "last_logout_time", System.currentTimeMillis());
    }
}

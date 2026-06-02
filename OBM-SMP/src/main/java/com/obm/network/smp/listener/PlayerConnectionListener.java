package com.obm.network.smp.listener;

import com.obm.network.core.OBMCorePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

public class PlayerConnectionListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        // ✅ GUARDA DADOS DO PLAYER
        OBMCorePlugin.get().getDataStore().save(player.getUniqueId());
    }
}
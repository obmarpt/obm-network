package com.obm.network.smp.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Persistência em quit é feita por {@link com.obm.network.core.listeners.CriticalPlayerSaveListener} (OBM-Core).
 */
public class PlayerConnectionListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Intencionalmente vazio — evita double-save; Core guarda inventário + economia + flush.
    }
}

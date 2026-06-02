package com.obm.network.inventory.listener;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.inventory.OBMInventoryPlugin;
import com.obm.network.inventory.manager.InventoryManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class InventoryListener implements Listener {

    private final InventoryManager manager;
    private final OBMInventoryPlugin plugin;

    public InventoryListener(OBMInventoryPlugin plugin, InventoryManager manager) {
        this.plugin = plugin;
        this.manager = manager;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
public void onWorldChange(PlayerChangedWorldEvent e) {

    Player player = e.getPlayer();

    // mundo anterior
    String from = e.getFrom().getName();

    // mundo atual
    String to = player.getWorld().getName();

    // guardar inventário antigo
    manager.saveInventory(player, from);

    // preserve fresh-entry state from menu before GlobalStorageListener clears it
    String lockKey = "location_restore_locked_" + player.getUniqueId();
    boolean skipRestore = OBMCorePlugin.get().getDataStore().getBoolean(player.getUniqueId(), lockKey);

    Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (skipRestore && (OBMCorePlugin.get().getWorldModeService().isUHC(to)
                    || OBMCorePlugin.get().getWorldModeService().isSMP(to))) {
                player.getInventory().clear();
            } else {
                manager.loadInventory(player, to);
            }

            String lobbyWorld = OBMCorePlugin.get().getWorldModeService().getLobbyWorld();
            if (lobbyWorld != null && to.equalsIgnoreCase(lobbyWorld)) {
                manager.giveCompass(player);
            }

        }, 6L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {

        Player p = e.getPlayer();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            manager.loadInventory(p, p.getWorld().getName());
        }, 6L);
    }
}
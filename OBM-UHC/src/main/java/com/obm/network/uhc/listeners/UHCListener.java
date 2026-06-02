package com.obm.network.uhc.listeners;

import com.obm.network.uhc.OBMUHCPlugin;
import com.obm.network.uhc.manager.UHCManager;
import com.obm.network.uhc.service.UHCReviveService;
import com.obm.network.uhc.util.UHCUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class UHCListener implements Listener {

    private final UHCManager manager;
    private final UHCReviveService reviveService;

    public UHCListener(UHCManager manager, UHCReviveService reviveService) {
        this.manager = manager;
        this.reviveService = reviveService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!UHCUtils.isUHC(player)) {
            return;
        }

        manager.handleJoin(player);
        reviveService.processPendingRevive(player);
        manager.ensureHardcoreWorld();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!UHCUtils.isUHC(player)) {
            return;
        }
        reviveService.processPendingRevive(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!UHCUtils.isUHC(player)) {
            return;
        }
        manager.handleQuit(player);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!UHCUtils.isUHC(victim)) {
            return;
        }
        manager.handleDeath(victim, victim.getKiller());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!UHCUtils.isUHC(player)) {
            return;
        }

        if (!manager.shouldSendToLobby(player)) {
            return;
        }

        manager.clearSendToLobbyFlag(player.getUniqueId());

        var lobbySpawn = manager.resolveLobbyRespawnLocation();
        if (lobbySpawn.isEmpty()) {
            player.sendMessage(Component.text("Erro: plugin OBM-Lobby ou mundo do lobby indisponível!", NamedTextColor.RED));
            return;
        }

        Location spawn = lobbySpawn.get();
        event.setRespawnLocation(spawn);

        org.bukkit.Bukkit.getScheduler().runTaskLater(OBMUHCPlugin.get(), () -> {
            if (player.isOnline()) {
                manager.openDeathMenu(player);
            }
        }, 10L);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().title().equals(Component.text("MORTE UHC", NamedTextColor.DARK_RED))) {
            event.setCancelled(true);
        }
    }
}

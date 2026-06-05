package com.obm.network.lobby.join;

import com.obm.network.core.location.LobbySpawnService;
import com.obm.network.core.state.PlayerStateBridge;
import com.obm.network.lobby.LobbyWorldService;
import com.obm.network.lobby.OBMLobbyPlugin;
import com.obm.network.lobby.items.LobbyItemsService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinHandler implements Listener {

    private final OBMLobbyPlugin plugin;

    public JoinHandler(OBMLobbyPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null);

        if (!LobbyWorldService.isInLobby(player)) {
            return;
        }

        applyLobbyState(player, true);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!LobbyWorldService.isInLobby(player)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && LobbyWorldService.isInLobby(player)) {
                applyLobbyState(player, false);
            }
        }, 3L);
    }

    private void applyLobbyState(Player player, boolean welcome) {
        LobbySpawnService.teleportToLobby(player);
        PlayerStateBridge.enterLobby(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !LobbyWorldService.isInLobby(player)) {
                return;
            }
            LobbyItemsService.equip(player);
            if (welcome) {
                WelcomeMessage.send(player);
            }
        }, 2L);

        // Após player-state-reset followup (tick ~10) — garante hotbar se reset limpar inventário
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && LobbyWorldService.isInLobby(player)) {
                LobbyItemsService.equip(player);
            }
        }, 12L);
    }
}

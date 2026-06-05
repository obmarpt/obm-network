package com.obm.network.lobby;

import com.obm.network.core.OBMCorePlugin;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Verifica se o jogador está no mundo lobby (OBM-Core + fallback OBM-Lobby config).
 */
public final class LobbyWorldService {

    private LobbyWorldService() {
    }

    public static String lobbyWorldName() {
        OBMCorePlugin core = OBMCorePlugin.get();
        if (core != null) {
            return core.getWorldModeService().getLobbyWorld();
        }
        OBMLobbyPlugin lobby = OBMLobbyPlugin.get();
        if (lobby != null) {
            return lobby.getConfig().getString("lobby.world", "Lobby");
        }
        return "Lobby";
    }

    public static boolean isLobbyWorld(World world) {
        if (world == null) {
            return false;
        }
        return world.getName().equalsIgnoreCase(lobbyWorldName());
    }

    public static boolean isInLobby(Player player) {
        return player != null && isLobbyWorld(player.getWorld());
    }
}

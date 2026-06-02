package com.obm.network.tierspace.anticheat;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MatchProtectionService {

    private static final float DEFAULT_WALK_SPEED = 0.2f;

    private final Map<UUID, Float> savedWalkSpeeds = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> countdownFrozen = new ConcurrentHashMap<>();

    public void onMatchStart(Player player) {
        if (player == null) {
            return;
        }
        applyMatchSafeEnvironment(player);
    }

    public void onMatchEnd(Player player) {
        if (player == null) {
            return;
        }
        releaseCountdownProtection(player);
    }

    public void applyMatchSafeEnvironment(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFallDistance(0);
    }

    public void applyCountdownProtection(Player player) {
        UUID uuid = player.getUniqueId();
        savedWalkSpeeds.putIfAbsent(uuid, player.getWalkSpeed());
        countdownFrozen.put(uuid, true);
        player.setWalkSpeed(0f);
        player.setInvulnerable(true);
    }

    public void releaseCountdownProtection(Player player) {
        UUID uuid = player.getUniqueId();
        countdownFrozen.remove(uuid);
        Float saved = savedWalkSpeeds.remove(uuid);
        player.setWalkSpeed(saved != null ? saved : DEFAULT_WALK_SPEED);
        player.setInvulnerable(false);
    }

    public boolean isCountdownFrozen(UUID uuid) {
        return countdownFrozen.containsKey(uuid);
    }
}

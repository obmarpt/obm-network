package com.obm.network.smp.shop.session;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopSessionManager {

    private final Map<UUID, ShopSession> sessions = new ConcurrentHashMap<>();

    public ShopSession getOrCreate(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), id -> new ShopSession(player));
    }

    public ShopSession get(UUID uuid) {
        return sessions.get(uuid);
    }

    public void remove(UUID uuid) {
        sessions.remove(uuid);
    }
}

package com.obm.network.uhc.manager;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;

import org.bukkit.entity.Player;

import java.util.UUID;

public class UHCManager {

    private final DataStore ds = OBMCorePlugin.get().getDataStore();

    private static final long PROTECTION_TIME = 10 * 60 * 1000L; // 10 minutos

    public boolean isUHC(Player p) {
        return OBMCorePlugin.get().getWorldModeService().isUHC(p.getWorld().getName());
    }

    // ✅ quando entra no UHC
    public void handleJoin(Player p) {
        UUID uuid = p.getUniqueId();

        if (!ds.has(uuid, "join_time_uhc")) {
            ds.set(uuid, "join_time_uhc", System.currentTimeMillis());
            ds.set(uuid, "lives_uhc", 1);
        }
    }

    // ✅ verifica se deve perder proteção (10m)
    public void checkProtection(Player p) {
        UUID uuid = p.getUniqueId();

        int lives = ds.getInt(uuid, "lives_uhc");
        if (lives <= 0) return;

        long joinTime = ds.getLong(uuid, "join_time_uhc");

        if (joinTime <= 0) {
            ds.set(uuid, "join_time_uhc", System.currentTimeMillis());
        }
    }

    // ✅ quando mata alguém
    public void handleKill(Player killer) {
        // Com apenas 1 vida no UHC, matar não remove vidas extras.
    }

    // ✅ quando morre
    public void handleDeath(Player p) {
        UUID uuid = p.getUniqueId();

        int lives = ds.getInt(uuid, "lives_uhc");

        lives--;
        ds.set(uuid, "lives_uhc", lives);
    }

    public int getLives(Player p) {
        return ds.getInt(p.getUniqueId(), "lives_uhc");
    }
}
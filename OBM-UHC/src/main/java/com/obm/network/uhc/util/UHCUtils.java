package com.obm.network.uhc.util;

import org.bukkit.entity.Player;
import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;

public class UHCUtils {

    private static final DataStore ds = OBMCorePlugin.get().getDataStore();
    
    private static final long PROTECTION_TIME_MS = 10L * 60L * 1000L; // 10 minutos
    
    private static final String LIVES_KEY = "lives_uhc";
    private static final String JOIN_TIME_KEY = "join_time_uhc";

    public static boolean isUHC(Player p) {
        if (p == null || p.getWorld() == null) return false;
        return OBMCorePlugin.get().getWorldModeService().isUHC(p.getWorld().getName());
    }

    public static int getLives(Player p) {
        return ds.getInt(p.getUniqueId(), LIVES_KEY);
    }

    public static void setLives(Player p, int lives) {
        int clampedLives = Math.max(0, Math.min(1, lives));
        ds.set(p.getUniqueId(), LIVES_KEY, clampedLives);
    }

    public static void giveFullLives(Player p) {
        setLives(p, 1);
        setJoinTime(p);
    }

    public static void setJoinTime(Player p) {
        ds.set(p.getUniqueId(), JOIN_TIME_KEY, System.currentTimeMillis());
    }

    public static long getJoinTime(Player p) {
        return ds.getLong(p.getUniqueId(), JOIN_TIME_KEY);
    }

    public static boolean hasProtection(Player p) {
        long joinTime = getJoinTime(p);

        if (joinTime <= 0) {
            setJoinTime(p);
            return true;
        }

        return System.currentTimeMillis() - joinTime < PROTECTION_TIME_MS;
    }

    public static long getRemainingProtection(Player p) {
        long joinTime = getJoinTime(p);
        if (joinTime <= 0) {
            return 0L;
        }

        long remainingMs = PROTECTION_TIME_MS - (System.currentTimeMillis() - joinTime);
        return Math.max(0L, remainingMs);
    }
}
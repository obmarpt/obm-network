package com.obm.network.lobby.mode;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.world.WorldModeService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Contagem de jogadores online por modo (cache leve, recalculada sob pedido).
 */
public final class ModeOnlineCounter {

    public enum Mode {
        SMP, HARDCORE, TIERSPACE
    }

    private static int cachedSmp = -1;
    private static int cachedHc = -1;
    private static int cachedTier = -1;
    private static long cacheTick = -1L;

    private ModeOnlineCounter() {
    }

    public static int count(Mode mode) {
        refreshIfStale();
        return switch (mode) {
            case SMP -> cachedSmp;
            case HARDCORE -> cachedHc;
            case TIERSPACE -> cachedTier;
        };
    }

    public static Mode busiest() {
        refreshIfStale();
        int max = Math.max(cachedSmp, Math.max(cachedHc, cachedTier));
        if (max <= 0) {
            return Mode.SMP;
        }
        if (cachedSmp == max) {
            return Mode.SMP;
        }
        if (cachedHc == max) {
            return Mode.HARDCORE;
        }
        return Mode.TIERSPACE;
    }

    public static void invalidate() {
        cacheTick = -1L;
    }

    private static void refreshIfStale() {
        long tick = Bukkit.getCurrentTick();
        if (cacheTick >= 0 && tick - cacheTick < 40L) {
            return;
        }
        cacheTick = tick;

        OBMCorePlugin core = OBMCorePlugin.get();
        if (core == null) {
            cachedSmp = cachedHc = cachedTier = 0;
            return;
        }

        WorldModeService wms = core.getWorldModeService();
        int smp = 0;
        int hc = 0;
        int tier = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            String world = online.getWorld().getName();
            if (wms.isSMP(world)) {
                smp++;
            } else if (wms.isUHC(world)) {
                hc++;
            } else if (wms.isRanked(world)) {
                tier++;
            }
        }
        cachedSmp = smp;
        cachedHc = hc;
        cachedTier = tier;
    }
}

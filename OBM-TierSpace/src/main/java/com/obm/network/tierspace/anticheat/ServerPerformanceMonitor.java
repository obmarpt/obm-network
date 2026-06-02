package com.obm.network.tierspace.anticheat;

import org.bukkit.Bukkit;

public final class ServerPerformanceMonitor {

    private ServerPerformanceMonitor() {
    }

    public static double getTps() {
        try {
            double[] tps = Bukkit.getServer().getTPS();
            if (tps != null && tps.length > 0) {
                return Math.min(20.0, tps[0]);
            }
        } catch (NoSuchMethodError ignored) {
        }
        return 20.0;
    }
}

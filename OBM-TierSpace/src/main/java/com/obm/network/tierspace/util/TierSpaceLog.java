package com.obm.network.tierspace.util;

import com.obm.network.tierspace.TierSpacePlugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class TierSpaceLog {

    private TierSpaceLog() {
    }

    public static void info(String message) {
        log(Level.INFO, message);
    }

    public static void warn(String message) {
        log(Level.WARNING, message);
    }

    public static void debug(String message) {
        if (!isDebug()) {
            return;
        }
        log(Level.INFO, "[debug] " + message);
    }

    private static void log(Level level, String message) {
        Logger logger = logger();
        if (logger != null) {
            logger.log(level, "[TierSpace] " + message);
        }
    }

    private static boolean isDebug() {
        TierSpacePlugin plugin = TierSpacePlugin.get();
        return plugin != null && plugin.getConfig().getBoolean("logging.debug", false);
    }

    private static Logger logger() {
        JavaPlugin plugin = TierSpacePlugin.get();
        return plugin != null ? plugin.getLogger() : null;
    }
}

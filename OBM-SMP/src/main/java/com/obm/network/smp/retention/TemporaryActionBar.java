package com.obm.network.smp.retention;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Action bar temporária — evita loops de refresh constante.
 */
public final class TemporaryActionBar {

    public static final int DEFAULT_TICKS = 80; // ~4 segundos

    private static final Map<UUID, BukkitTask> CLEAR_TASKS = new ConcurrentHashMap<>();

    private TemporaryActionBar() {
    }

    public static void show(Plugin plugin, Player player, String message) {
        show(plugin, player, message, DEFAULT_TICKS);
    }

    public static void show(Plugin plugin, Player player, String message, int displayTicks) {
        if (player == null || message == null || message.isBlank()) {
            return;
        }
        cancel(player.getUniqueId());
        RetentionFeedback.sendActionBar(player, message);
        int ticks = Math.max(20, displayTicks);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> clear(player), ticks);
        CLEAR_TASKS.put(player.getUniqueId(), task);
    }

    public static void clear(Player player) {
        if (player == null) {
            return;
        }
        cancel(player.getUniqueId());
        try {
            player.sendActionBar(" ");
        } catch (NoSuchMethodError ignored) {
            // legacy
        }
    }

    private static void cancel(UUID uuid) {
        BukkitTask existing = CLEAR_TASKS.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }
    }
}

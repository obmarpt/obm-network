package com.obm.network.smp.spawn;

import com.obm.network.smp.service.SpawnProtectionService;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnBoundaryManager {

    private final JavaPlugin plugin;
    private final SpawnProtectionService protection;
    private final SpawnBoundaryBlockService blocks;
    private final SpawnBoundaryVisualizer visualizer;
    private BukkitTask task;

    public SpawnBoundaryManager(JavaPlugin plugin, SpawnProtectionService protection) {
        this.plugin = plugin;
        this.protection = protection;
        this.blocks = new SpawnBoundaryBlockService(protection);
        this.visualizer = new SpawnBoundaryVisualizer(protection);
    }

    public void start() {
        stop();
        if (!protection.isEnabled()) {
            return;
        }
        SpawnBoundarySettings settings = protection.settings();
        if (settings.showBoundaryBlocks() && settings.regenerateOnRestart()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, blocks::placeBoundaryRing, 40L);
        }
        if (settings.showBoundary() && (settings.showParticles() || settings.showDistanceWarning())) {
            long interval = settings.particleIntervalTicks();
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, visualizer::tick, interval, interval);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        blocks.clearBoundary();
    }

    public SpawnBoundaryBlockService blocks() {
        return blocks;
    }
}

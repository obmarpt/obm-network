package com.obm.network.smp.service;

import com.obm.network.core.world.WorldModeService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SpawnProtectionService {

    private final WorldModeService worldModeService;
    private final double radiusSquared;
    private final boolean useWorldSpawn;
    private final String configuredWorld;
    private final double centerX;
    private final double centerY;
    private final double centerZ;

    public SpawnProtectionService(FileConfiguration config, WorldModeService worldModeService) {
        this.worldModeService = worldModeService;
        double radius = config.getDouble("spawn.protection-radius", 100);
        this.radiusSquared = radius * radius;
        this.useWorldSpawn = config.getBoolean("spawn.center.use-world-spawn", true);
        this.configuredWorld = worldModeService.getPrimarySMPWorld();
        this.centerX = config.getDouble("spawn.center.x", 0);
        this.centerY = config.getDouble("spawn.center.y", 64);
        this.centerZ = config.getDouble("spawn.center.z", 0);
    }

    public boolean isInSpawnProtection(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!worldModeService.isSMP(location.getWorld().getName())) {
            return false;
        }

        Location center = resolveCenter(location.getWorld());
        if (center == null) {
            return false;
        }
        if (!center.getWorld().equals(location.getWorld())) {
            return false;
        }

        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return (dx * dx + dz * dz) <= radiusSquared;
    }

    public boolean isPvpAllowed(Player attacker, Player victim) {
        if (attacker == null || victim == null) {
            return true;
        }
        if (!worldModeService.isSMP(attacker.getWorld().getName())) {
            return true;
        }
        if (isInSpawnProtection(attacker.getLocation()) || isInSpawnProtection(victim.getLocation())) {
            return false;
        }
        return true;
    }

    public Location getSpawnCenter() {
        World world = Bukkit.getWorld(configuredWorld);
        if (world == null) {
            return null;
        }
        return resolveCenter(world);
    }

    private Location resolveCenter(World world) {
        if (useWorldSpawn) {
            return world.getSpawnLocation();
        }
        return new Location(world, centerX, centerY, centerZ);
    }
}

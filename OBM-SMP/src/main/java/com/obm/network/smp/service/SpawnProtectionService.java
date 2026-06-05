package com.obm.network.smp.service;

import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.spawn.SpawnBoundarySettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SpawnProtectionService {

    private final WorldModeService worldModeService;
    private final SpawnBoundarySettings settings;
    private final double radius;
    private final double radiusSquared;

    public SpawnProtectionService(FileConfiguration config, WorldModeService worldModeService) {
        this.worldModeService = worldModeService;
        double legacyRadius = config.getDouble("spawn.protection-radius", 100);
        this.settings = SpawnBoundarySettings.from(config, worldModeService.getPrimarySMPWorld(), legacyRadius);
        this.radius = settings.radius();
        this.radiusSquared = radius * radius;
    }

    public SpawnBoundarySettings settings() {
        return settings;
    }

    public double getRadius() {
        return radius;
    }

    public boolean isEnabled() {
        return settings.enabled();
    }

    public boolean isInSpawnProtection(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!worldModeService.isSMP(location.getWorld().getName())) {
            return false;
        }

        Location center = resolveCenter(location.getWorld());
        if (center == null || !center.getWorld().equals(location.getWorld())) {
            return false;
        }

        return horizontalDistanceSquared(location, center) <= radiusSquared;
    }

    public double horizontalDistance(Location location) {
        Location center = resolveCenter(location.getWorld());
        if (center == null) {
            return Double.MAX_VALUE;
        }
        return Math.sqrt(horizontalDistanceSquared(location, center));
    }

    public boolean isNearBoundary(Location location) {
        if (!isInSpawnProtection(location)) {
            return false;
        }
        double dist = horizontalDistance(location);
        return dist >= radius - settings.proximityWarningBlocks();
    }

    public boolean isOutsideBoundary(Location location) {
        if (location == null || location.getWorld() == null) {
            return true;
        }
        if (!worldModeService.isSMP(location.getWorld().getName())) {
            return false;
        }
        Location center = resolveCenter(location.getWorld());
        if (center == null) {
            return false;
        }
        return horizontalDistanceSquared(location, center) > radiusSquared;
    }

    public Location clampInside(Location location) {
        Location center = resolveCenter(location.getWorld());
        if (center == null) {
            return location;
        }
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist <= radius - 0.5 || dist < 0.001) {
            return location;
        }
        double scale = (radius - 0.5) / dist;
        return new Location(
                location.getWorld(),
                center.getX() + dx * scale,
                location.getY(),
                center.getZ() + dz * scale,
                location.getYaw(),
                location.getPitch()
        );
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
        World world = Bukkit.getWorld(settings.world());
        if (world == null) {
            world = Bukkit.getWorld(worldModeService.getPrimarySMPWorld());
        }
        if (world == null) {
            return null;
        }
        return resolveCenter(world);
    }

    public Location resolveCenter(World world) {
        if (world == null) {
            return null;
        }
        if (settings.useWorldSpawn()) {
            return world.getSpawnLocation();
        }
        return new Location(world, settings.centerX(), settings.centerY(), settings.centerZ());
    }

    public boolean isBoundaryWorld(World world) {
        if (world == null) {
            return false;
        }
        if (!worldModeService.isSMP(world.getName())) {
            return false;
        }
        Location center = resolveCenter(world);
        return center != null && center.getWorld().equals(world);
    }

    private double horizontalDistanceSquared(Location a, Location center) {
        double dx = a.getX() - center.getX();
        double dz = a.getZ() - center.getZ();
        return dx * dx + dz * dz;
    }
}

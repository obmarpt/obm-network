package com.obm.network.smp.spawn;

import com.obm.network.smp.service.SpawnProtectionService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public final class SpawnBoundaryBlockService {

    private final SpawnProtectionService protection;
    private final Set<String> boundaryKeys = new HashSet<>();

    public SpawnBoundaryBlockService(SpawnProtectionService protection) {
        this.protection = protection;
    }

    public void placeBoundaryRing() {
        clearBoundary();
        SpawnBoundarySettings settings = protection.settings();
        if (!settings.showBoundaryBlocks() || !settings.regenerateOnRestart()) {
            return;
        }
        Location center = protection.getSpawnCenter();
        if (center == null || center.getWorld() == null) {
            return;
        }
        World world = center.getWorld();
        double radius = protection.getRadius();
        int points = Math.max(32, settings.particlePoints());
        int baseY = center.getBlockY();

        if (settings.circleShape()) {
            for (int i = 0; i < points; i++) {
                double angle = (2.0 * Math.PI * i) / points;
                int x = (int) Math.floor(center.getX() + radius * Math.cos(angle));
                int z = (int) Math.floor(center.getZ() + radius * Math.sin(angle));
                placeColumn(world, x, baseY, z, settings);
            }
        } else {
            int r = (int) Math.ceil(radius);
            int cx = center.getBlockX();
            int cz = center.getBlockZ();
            for (int x = cx - r; x <= cx + r; x++) {
                placeColumn(world, x, baseY, cz - r, settings);
                placeColumn(world, x, baseY, cz + r, settings);
            }
            for (int z = cz - r + 1; z < cz + r; z++) {
                placeColumn(world, cx - r, baseY, z, settings);
                placeColumn(world, cx + r, baseY, z, settings);
            }
        }
    }

    public void clearBoundary() {
        for (String key : new HashSet<>(boundaryKeys)) {
            String[] parts = key.split(",");
            if (parts.length != 4) {
                continue;
            }
            World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null) {
                continue;
            }
            Block block = world.getBlockAt(
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
            if (boundaryKeys.contains(key)) {
                block.setType(Material.AIR, false);
            }
        }
        boundaryKeys.clear();
    }

    public boolean isBoundaryBlock(Block block) {
        if (block == null) {
            return false;
        }
        return boundaryKeys.contains(key(block.getLocation()));
    }

    private void placeColumn(World world, int x, int baseY, int z, SpawnBoundarySettings settings) {
        Material mat = settings.blockType();
        for (int h = 0; h < settings.blockHeight(); h++) {
            Block block = world.getBlockAt(x, baseY + h, z);
            if (!canPlace(block, settings.replaceAirOnly())) {
                continue;
            }
            block.setType(mat, false);
            boundaryKeys.add(key(block.getLocation()));
        }
    }

    private boolean canPlace(Block block, boolean airOnly) {
        Material type = block.getType();
        if (airOnly) {
            return type.isAir() || type == Material.SHORT_GRASS || type == Material.TALL_GRASS
                    || type == Material.FERN || type == Material.LARGE_FERN
                    || type == Material.DEAD_BUSH || type == Material.SNOW;
        }
        return type.isAir() || !type.isSolid();
    }

    private static String key(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}

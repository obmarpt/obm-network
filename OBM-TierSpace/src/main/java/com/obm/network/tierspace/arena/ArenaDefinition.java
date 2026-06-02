package com.obm.network.tierspace.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public record ArenaDefinition(
        String id,
        Location spawn1,
        Location spawn2,
        List<String> modes,
        boolean autoReset,
        Location resetMin,
        Location resetMax
) {
    public boolean supports(String modeId) {
        if (modes == null || modes.isEmpty() || modes.contains("*")) {
            return true;
        }
        return modes.stream().anyMatch(mode -> mode.equalsIgnoreCase(modeId));
    }

    public static ArenaDefinition fromConfig(String id, ConfigurationSection section, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null || section == null) {
            return null;
        }
        Location first = readSpawn(section.getConfigurationSection("spawn1"), world);
        Location second = readSpawn(section.getConfigurationSection("spawn2"), world);
        if (first == null || second == null) {
            return null;
        }

        List<String> modes = section.getStringList("modes");
        if (modes.isEmpty()) {
            modes = List.of("*");
        }

        boolean autoReset = section.getBoolean("auto-reset", false);
        Location resetMin = readCorner(section.getConfigurationSection("reset-min"), world);
        Location resetMax = readCorner(section.getConfigurationSection("reset-max"), world);

        return new ArenaDefinition(id, first, second, modes, autoReset, resetMin, resetMax);
    }

    public void resetBlocks() {
        if (!autoReset || resetMin == null || resetMax == null || resetMin.getWorld() == null) {
            return;
        }

        World world = resetMin.getWorld();
        int minX = Math.min(resetMin.getBlockX(), resetMax.getBlockX());
        int maxX = Math.max(resetMin.getBlockX(), resetMax.getBlockX());
        int minY = Math.min(resetMin.getBlockY(), resetMax.getBlockY());
        int maxY = Math.max(resetMin.getBlockY(), resetMax.getBlockY());
        int minZ = Math.min(resetMin.getBlockZ(), resetMax.getBlockZ());
        int maxZ = Math.max(resetMin.getBlockZ(), resetMax.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (isResetMaterial(block.getType())) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private static boolean isResetMaterial(Material type) {
        return type == Material.OBSIDIAN
                || type == Material.CRYING_OBSIDIAN
                || type == Material.END_CRYSTAL
                || type == Material.RESPAWN_ANCHOR
                || type == Material.GLOWSTONE
                || type == Material.TNT
                || type == Material.FIRE
                || type == Material.SOUL_FIRE;
    }

    private static Location readSpawn(ConfigurationSection section, World world) {
        if (section == null) {
            return null;
        }
        return new Location(
                world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }

    private static Location readCorner(ConfigurationSection section, World world) {
        if (section == null) {
            return null;
        }
        return new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"));
    }
}

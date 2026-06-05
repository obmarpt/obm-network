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
        String name,
        Location spawn1,
        Location spawn2,
        List<String> modes,
        boolean enabled,
        boolean autoReset,
        Location resetMin,
        Location resetMax
) {
    public boolean supports(String modeId) {
        if (!enabled) {
            return false;
        }
        if (modes == null || modes.isEmpty() || modes.contains("*")) {
            return true;
        }
        return modes.stream().anyMatch(mode -> mode.equalsIgnoreCase(modeId));
    }

    public static ArenaDefinition fromConfig(String id, ConfigurationSection section, String defaultWorld) {
        World world = resolveWorld(section.getString("world", defaultWorld));
        if (world == null || section == null) {
            return null;
        }
        Location first = readSpawn(section.getConfigurationSection("spawn1"), world);
        Location second = readSpawn(section.getConfigurationSection("spawn2"), world);
        if (first == null || second == null) {
            return null;
        }

        List<String> modes = new ArrayList<>(section.getStringList("modes"));
        if (modes.isEmpty()) {
            String single = section.getString("mode");
            if (single != null && !single.isBlank()) {
                modes.add(single);
            } else {
                modes.add("*");
            }
        }

        boolean enabled = section.getBoolean("enabled", true);
        boolean autoReset = section.getBoolean("auto-reset", false);
        Location resetMin = readCorner(section.getConfigurationSection("reset-min"), world);
        Location resetMax = readCorner(section.getConfigurationSection("reset-max"), world);
        String name = section.getString("name", id);

        return new ArenaDefinition(id, name, first, second, modes, enabled, autoReset, resetMin, resetMax);
    }

    public static ArenaDefinition fromBackend(
            String id,
            String name,
            String mode,
            String worldName,
            Location spawn1,
            Location spawn2,
            boolean enabled,
            boolean autoReset) {
        if (spawn1 == null || spawn2 == null) {
            return null;
        }
        return new ArenaDefinition(
                id,
                name != null ? name : id,
                spawn1,
                spawn2,
                List.of(mode != null ? mode : "sword"),
                enabled,
                autoReset,
                null,
                null
        );
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

    private static World resolveWorld(String worldName) {
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return world;
        }
        return Bukkit.getWorlds().stream()
                .filter(w -> w.getName().equalsIgnoreCase(worldName))
                .findFirst()
                .orElse(null);
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

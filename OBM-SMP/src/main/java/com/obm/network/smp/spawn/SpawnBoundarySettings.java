package com.obm.network.smp.spawn;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;

public record SpawnBoundarySettings(
        boolean enabled,
        boolean useWorldSpawn,
        String world,
        double centerX,
        double centerY,
        double centerZ,
        double radius,
        boolean circleShape,
        boolean blockExit,
        boolean showBoundary,
        boolean showBoundaryBlocks,
        boolean showParticles,
        Particle particleType,
        int particlePoints,
        int particleIntervalTicks,
        double renderDistance,
        int proximityWarningBlocks,
        boolean showDistanceWarning,
        Material blockType,
        int blockHeight,
        boolean regenerateOnRestart,
        boolean replaceAirOnly
) {

    public static SpawnBoundarySettings from(FileConfiguration config, String defaultWorld, double legacyRadius) {
        var section = config.getConfigurationSection("spawn-protection");
        boolean useWorldSpawn = section != null
                ? section.getBoolean("use-world-spawn", config.getBoolean("spawn.center.use-world-spawn", true))
                : config.getBoolean("spawn.center.use-world-spawn", true);

        double radius = legacyRadius;
        if (section != null && section.contains("radius")) {
            radius = section.getDouble("radius", legacyRadius);
        } else {
            radius = config.getDouble("spawn.protection-radius", legacyRadius);
        }

        String world = section != null
                ? section.getString("world", defaultWorld)
                : defaultWorld;

        double x = section != null ? section.getDouble("x", config.getDouble("spawn.center.x", 0)) : config.getDouble("spawn.center.x", 0);
        double y = section != null ? section.getDouble("y", config.getDouble("spawn.center.y", 64)) : config.getDouble("spawn.center.y", 64);
        double z = section != null ? section.getDouble("z", config.getDouble("spawn.center.z", 0)) : config.getDouble("spawn.center.z", 0);

        boolean enabled = section == null || section.getBoolean("enabled", true);
        String shape = section != null ? section.getString("boundary-shape", "circle") : "circle";
        boolean circle = !"square".equalsIgnoreCase(shape);

        boolean showBoundary = section == null || section.getBoolean("show-boundary", true);
        boolean showBlocks = section != null && section.getBoolean("show-boundary-blocks", true);
        boolean showParticles = section == null || section.getBoolean("show-particles", true);

        Particle particle = parseParticle(section != null ? section.getString("particle-type", "END_ROD") : "END_ROD");
        int points = parseDensity(section != null ? section.getString("particle-density", "medium") : "medium");
        int interval = section != null
                ? (int) Math.max(20L, section.getLong("particle-interval-ticks", 20L))
                : 15;
        double renderDist = section != null ? section.getDouble("render-distance", 50.0) : 50.0;
        int warnBlocks = section != null ? section.getInt("proximity-warning-blocks", 3) : 3;
        boolean showWarn = section == null || section.getBoolean("show-distance-warning", true);
        boolean blockExit = section != null && section.getBoolean("block-exit", false);

        Material blockMat = parseMaterial(
                section != null ? section.getString("block-type", "RED_STAINED_GLASS") : "RED_STAINED_GLASS",
                Material.RED_STAINED_GLASS);
        int blockHeight = section != null ? Math.max(1, section.getInt("block-height", 1)) : 1;
        boolean regen = section == null || section.getBoolean("regenerate-on-restart", true);
        boolean airOnly = section == null || section.getBoolean("boundary-only-replace-air", true);

        return new SpawnBoundarySettings(
                enabled,
                useWorldSpawn,
                world,
                x, y, z,
                Math.max(5.0, radius),
                circle,
                blockExit,
                showBoundary,
                showBlocks,
                showParticles,
                particle,
                points,
                interval,
                Math.max(20.0, renderDist),
                Math.max(1, warnBlocks),
                showWarn,
                blockMat,
                blockHeight,
                regen,
                airOnly
        );
    }

    private static Particle parseParticle(String name) {
        if (name == null || name.isBlank()) {
            return Particle.END_ROD;
        }
        String normalized = name.trim().toUpperCase().replace(' ', '_');
        if ("REDSTONE".equals(normalized)) {
            return Particle.DUST;
        }
        try {
            return Particle.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return Particle.END_ROD;
        }
    }

    private static int parseDensity(String density) {
        if (density == null) {
            return 64;
        }
        return switch (density.toLowerCase()) {
            case "low" -> 40;
            case "high" -> 96;
            default -> 64;
        };
    }

    private static Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Material mat = Material.matchMaterial(name.trim().toUpperCase());
        return mat != null && mat.isBlock() ? mat : fallback;
    }
}

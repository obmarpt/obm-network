package com.obm.network.smp.duel;

import com.obm.network.smp.SMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class DuelArenaService {

    private final SMPPlugin plugin;
    private String worldName;
    private Location spawnOne;
    private Location spawnTwo;

    public DuelArenaService(SMPPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        var cfg = plugin.getConfig();
        this.worldName = cfg.getString("duel.world", "duel");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[Duel] Mundo '" + worldName + "' não encontrado. Cria o mundo ou ajusta duel.world.");
            spawnOne = null;
            spawnTwo = null;
            return;
        }
        spawnOne = readSpawn(world, cfg.getConfigurationSection("duel.spawn1"), 0, 64, 0, 0, 0);
        spawnTwo = readSpawn(world, cfg.getConfigurationSection("duel.spawn2"), 10, 64, 0, 180, 0);
    }

    public String worldName() {
        return worldName;
    }

    public boolean isReady() {
        return spawnOne != null && spawnTwo != null;
    }

    public boolean isDuelWorld(String world) {
        return world != null && world.equalsIgnoreCase(worldName);
    }

    public Location spawnOne() {
        return spawnOne == null ? null : spawnOne.clone();
    }

    public Location spawnTwo() {
        return spawnTwo == null ? null : spawnTwo.clone();
    }

    private static Location readSpawn(World world, ConfigurationSection section,
                                      double dx, double dy, double dz, float yaw, float pitch) {
        double x = section != null ? section.getDouble("x", dx) : dx;
        double y = section != null ? section.getDouble("y", dy) : dy;
        double z = section != null ? section.getDouble("z", dz) : dz;
        float syaw = section != null ? (float) section.getDouble("yaw", yaw) : yaw;
        float spitch = section != null ? (float) section.getDouble("pitch", pitch) : pitch;
        return new Location(world, x, y, z, syaw, spitch);
    }
}

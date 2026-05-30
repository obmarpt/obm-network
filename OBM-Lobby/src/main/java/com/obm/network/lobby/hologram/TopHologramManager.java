package com.obm.network.lobby.hologram;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

import com.obm.network.lobby.OBMLobbyPlugin;
import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;

public class TopHologramManager {

    private final OBMLobbyPlugin plugin;

    // ✅ guardar hologramas (ESSENCIAL)
    private final Map<String, Hologram> holograms = new HashMap<>();

    public TopHologramManager(OBMLobbyPlugin plugin) {
        this.plugin = plugin;
    }

    public void createAll() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            createTop("top_smp_playtime", "§a§lTOP SMP PLAYTIME",
                    getLocation("smp_playtime"), "playtime_smp");

            createTop("top_smp_blocks", "§e§lTOP SMP BLOCKS",
                    getLocation("smp_blocks"), "blocks_smp");

            createTop("top_smp_kills", "§c§lTOP SMP KILLS",
                    getLocation("smp_kills"), "kills_smp");

            createTop("top_smp_money", "§6§lTOP MONEY",
                    getLocation("smp_money"), "money_smp");

            createTop("top_uhc_kills", "§c§lTOP UHC KILLS",
                    getLocation("uhc_kills"), "kills_uhc");

            createTop("top_uhc_deaths", "§4§lTOP UHC DEATHS",
                    getLocation("uhc_deaths"), "deaths_uhc");

            createTop("top_uhc_playtime", "§6§lTOP UHC PLAYTIME",
                    getLocation("uhc_playtime"), "playtime_uhc");

            createTop("top_uhc_lives", "§4§lTOP UHC LIVES",
                    getLocation("uhc_lives"), "lives_uhc");

            createKDR("top_uhc_kdr", getLocation("uhc_kdr"));

        }, 0L, 200L);
    }

    private void createTop(String id, String title, Location loc, String statKey) {

        if (loc == null || loc.getWorld() == null) return;

        Hologram hologram = holograms.get(id);

        // ✅ criar uma única vez
        if (hologram == null) {
            hologram = DHAPI.createHologram(id, loc);
            holograms.put(id, hologram);

            // estrutura fixa (12 linhas)
            for (int i = 0; i < 12; i++) {
                DHAPI.addHologramLine(hologram, "");
            }
        } else {
            hologram.setLocation(loc);
        }

        DataStore ds = OBMCorePlugin.get().getDataStore();

        Map<UUID, Integer> map = new HashMap<>();

        if (ds.getYaml().getConfigurationSection("players") == null) return;

        for (String key : ds.getYaml()
                .getConfigurationSection("players")
                .getKeys(false)) {

            UUID uuid = UUID.fromString(key);
            int value = ds.getInt(uuid, statKey);

            if (value <= 0) continue;

            map.put(uuid, value);
        }

        List<Map.Entry<UUID, Integer>> top = map.entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        // ✅ atualizar linhas SEM recriar holograma
        updateLine(hologram, 0, title);
        updateLine(hologram, 1, "§7━━━━━━━━━━━━");

        int index = 2;
        int place = 1;

        for (Map.Entry<UUID, Integer> entry : top) {

            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "Unknown";

            int value = entry.getValue();

            String formatted = statKey.contains("playtime")
                    ? formatTime(value)
                    : String.valueOf(value);

            updateLine(hologram, index,
                    "§e#" + place + " §7" + name + " §8- §a" + formatted);

            index++;
            place++;
        }

        // limpar linhas restantes
        for (int i = index; i < 12; i++) {
            updateLine(hologram, i, "");
        }
    }

    private void createKDR(String id, Location loc) {

        if (loc == null || loc.getWorld() == null) return;

        Hologram hologram = holograms.get(id);

        if (hologram == null) {
            hologram = DHAPI.createHologram(id, loc);
            holograms.put(id, hologram);

            for (int i = 0; i < 12; i++) {
                DHAPI.addHologramLine(hologram, "");
            }
        } else {
            hologram.setLocation(loc);
        }

        DataStore ds = OBMCorePlugin.get().getDataStore();

        Map<UUID, Double> map = new HashMap<>();

        if (ds.getYaml().getConfigurationSection("players") == null) return;

        for (String key : ds.getYaml()
                .getConfigurationSection("players")
                .getKeys(false)) {

            UUID uuid = UUID.fromString(key);

            int kills = ds.getInt(uuid, "kills_uhc");
            int deaths = ds.getInt(uuid, "deaths_uhc");

            if (kills < 5) continue;

            double kdr = deaths == 0 ? kills : (double) kills / deaths;

            map.put(uuid, kdr);
        }

        List<Map.Entry<UUID, Double>> top = map.entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        updateLine(hologram, 0, "§c§lTOP KDR");
        updateLine(hologram, 1, "§7━━━━━━━━━━━━");

        int index = 2;
        int place = 1;

        for (Map.Entry<UUID, Double> entry : top) {

            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "Unknown";

            updateLine(hologram, index,
                    "§e#" + place + " §7" + name + " §8- §c"
                            + String.format("%.2f", entry.getValue()));

            index++;
            place++;
        }

        for (int i = index; i < 12; i++) {
            updateLine(hologram, i, "");
        }
    }

   private void updateLine(Hologram hologram, int index, String text) {

    int size = hologram.getPage(0).getLines().size();

    if (index < size) {
        DHAPI.setHologramLine(hologram, index, text);
    } else {
        DHAPI.addHologramLine(hologram, text);
    }
}

    private Location getLocation(String path) {
        String worldName = plugin.getConfig().getString("holograms." + path + ".world");
        double x = plugin.getConfig().getDouble("holograms." + path + ".x");
        double y = plugin.getConfig().getDouble("holograms." + path + ".y");
        double z = plugin.getConfig().getDouble("holograms." + path + ".z");

        World world = Bukkit.getWorld(worldName);

        if (world == null) return null;

        return new Location(world, x, y, z);
    }

    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }
}
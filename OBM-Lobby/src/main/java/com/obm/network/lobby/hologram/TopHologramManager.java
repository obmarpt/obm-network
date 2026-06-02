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
import com.obm.network.core.tier.TierRankUtil;

public class TopHologramManager {

    private final OBMLobbyPlugin plugin;

    // ✅ guardar hologramas (ESSENCIAL)
    private final Map<String, Hologram> holograms = new HashMap<>();

    public TopHologramManager(OBMLobbyPlugin plugin) {
        this.plugin = plugin;
    }

    public void createAll() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            createTop("top_smp_playtime", tierTitle("📊 TOP RUSH — PLAYTIME", "#FFAA00", "#FF7700"),
                    getLocation("smp_playtime"), "playtime_smp", rushFooter());

            createTop("top_smp_blocks", tierTitle("⛏ TOP RUSH — BLOCKS", "#FFAA00", "#FF7700"),
                    getLocation("smp_blocks"), "blocks_smp", rushFooter());

            createTop("top_smp_kills", tierTitle("⚔ TOP RUSH — KILLS", "#FFAA00", "#FF7700"),
                    getLocation("smp_kills"), "kills_smp", rushFooter());

            createTop("top_smp_money", tierTitle("💰 TOP RUSH — COINS", "#FFAA00", "#FF7700"),
                    getLocation("smp_money"), "money_smp", rushFooter());

            createTop("top_uhc_kills", tierTitle("⚔ TOP HARDCORE — KILLS", "#FF4444", "#880000"),
                    getLocation("uhc_kills"), "kills_uhc", hardcoreFooter());

            createTop("top_uhc_deaths", tierTitle("💀 TOP HARDCORE — DEATHS", "#FF4444", "#880000"),
                    getLocation("uhc_deaths"), "deaths_uhc", hardcoreFooter());

            createTop("top_uhc_playtime", tierTitle("⏳ TOP HARDCORE — PLAYTIME", "#FF4444", "#880000"),
                    getLocation("uhc_playtime"), "playtime_uhc", hardcoreFooter());

            createTop("top_uhc_lives", tierTitle("❤ TOP HARDCORE — VIDAS", "#FF4444", "#880000"),
                    getLocation("uhc_lives"), "lives_uhc", hardcoreFooter());

            createTierTop("top_tierspace_rating",
                    tierTitle("🏆 TIER TOP — RATING", "#00AAFF", "#AA00FF"),
                    getLocation("tierspace_rating"), "tierspace_sword_rating", true,
                    tierFooter());

            createTierTop("top_tierspace_streak",
                    tierTitle("🔥 TOP STREAK", "#AA00FF", "#FF4444"),
                    getLocation("tierspace_streak"), "tierspace_sword_best_streak", false,
                    streakFooter());

            createKDR("top_uhc_kdr", getLocation("uhc_kdr"));

        }, 0L, 200L);
    }

    private static final int HOLO_LINES = 14;
    private static final String SEPARATOR = "<dark_gray>──────────────</dark_gray>";

    private void createTop(String id, String title, Location loc, String statKey, String footer) {

        if (loc == null || loc.getWorld() == null) return;

        Hologram hologram = holograms.get(id);

        // ✅ criar uma única vez
        if (hologram == null) {
            hologram = DHAPI.createHologram(id, loc);
            holograms.put(id, hologram);

            // estrutura fixa (12 linhas)
            for (int i = 0; i < HOLO_LINES; i++) {
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
        updateLine(hologram, 1, SEPARATOR);

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
                    formatPlaceLine(place, name, formatted));

            index++;
            place++;
        }

        while (index < 12) {
            updateLine(hologram, index, emptyPlaceLine(index - 1));
            index++;
        }

        updateLine(hologram, 12, SEPARATOR);
        updateLine(hologram, 13, footer);

        for (int i = HOLO_LINES; i < hologram.getPage(0).getLines().size(); i++) {
            updateLine(hologram, i, "");
        }
    }

    private void createTierTop(String id, String title, Location loc, String statKey, boolean showRank,
                               String footer) {

        if (loc == null || loc.getWorld() == null) return;

        Hologram hologram = holograms.get(id);

        if (hologram == null) {
            hologram = DHAPI.createHologram(id, loc);
            holograms.put(id, hologram);

            for (int i = 0; i < HOLO_LINES; i++) {
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

        updateLine(hologram, 0, title);
        updateLine(hologram, 1, SEPARATOR);

        int index = 2;
        int place = 1;

        for (Map.Entry<UUID, Integer> entry : top) {

            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "Unknown";

            int value = entry.getValue();
            String formatted = showRank
                    ? TierRankUtil.fromRating(value).plainName() + " <gray>(</gray><yellow>" + value + "</yellow><gray>)</gray>"
                    : String.valueOf(value);

            updateLine(hologram, index,
                    formatPlaceLine(place, name, formatted));

            index++;
            place++;
        }

        while (index < 12) {
            updateLine(hologram, index, emptyPlaceLine(index - 1));
            index++;
        }

        updateLine(hologram, 12, SEPARATOR);
        updateLine(hologram, 13, footer);

        for (int i = HOLO_LINES; i < hologram.getPage(0).getLines().size(); i++) {
            updateLine(hologram, i, "");
        }
    }

    private void createKDR(String id, Location loc) {

        if (loc == null || loc.getWorld() == null) return;

        Hologram hologram = holograms.get(id);

        if (hologram == null) {
            hologram = DHAPI.createHologram(id, loc);
            holograms.put(id, hologram);

            for (int i = 0; i < HOLO_LINES; i++) {
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

        updateLine(hologram, 0, tierTitle("📊 TOP HARDCORE — K/D", "#FF4444", "#880000"));
        updateLine(hologram, 1, SEPARATOR);

        int index = 2;
        int place = 1;

        for (Map.Entry<UUID, Double> entry : top) {

            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "Unknown";

            updateLine(hologram, index,
                    formatPlaceLine(place, name, String.format("%.2f", entry.getValue())));

            index++;
            place++;
        }

        while (index < 12) {
            updateLine(hologram, index, emptyPlaceLine(index - 1));
            index++;
        }

        updateLine(hologram, 12, SEPARATOR);
        updateLine(hologram, 13, hardcoreFooter());
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

    private String formatPlaceLine(int place, String name, String value) {
        String prefix = switch (place) {
            case 1 -> "<gold><bold>#1</bold></gold>";
            case 2 -> "<gray><bold>#2</bold></gray>";
            case 3 -> "<red><bold>#3</bold></red>";
            default -> "<dark_gray>#" + place + "</dark_gray>";
        };
        return prefix + " <white>" + name + "</white> <dark_gray>—</dark_gray> <yellow>" + value + "</yellow>";
    }

    private String emptyPlaceLine(int rankIndex) {
        int place = rankIndex + 1;
        String prefix = switch (place) {
            case 1 -> "<gold><bold>#1</bold></gold>";
            case 2 -> "<gray><bold>#2</bold></gray>";
            case 3 -> "<red><bold>#3</bold></red>";
            default -> "<dark_gray>#" + place + "</dark_gray>";
        };
        return prefix + " <gray>—</gray> <dark_gray>—</dark_gray>";
    }

    private String tierTitle(String text, String from, String to) {
        return "<gradient:" + from + ":" + to + "><bold>" + text + "</bold></gradient>";
    }

    private String tierFooter() {
        return "<gradient:#00AAFF:#AA00FF>⚡ Sobe no ranking!</gradient>";
    }

    private String streakFooter() {
        return "<gradient:#AA00FF:#FF4444>🔥 Mantém a streak!</gradient>";
    }

    private String rushFooter() {
        return "<gradient:#FFAA00:#FF7700>💰 Domina a economia!</gradient>";
    }

    private String hardcoreFooter() {
        return "<gradient:#FF4444:#880000>💀 Sobrevive ao Hardcore!</gradient>";
    }

    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }
}
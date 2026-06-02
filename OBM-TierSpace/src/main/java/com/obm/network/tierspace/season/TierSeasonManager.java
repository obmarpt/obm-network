package com.obm.network.tierspace.season;

import com.obm.network.core.tier.TierRankUtil;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.storage.TierSpaceStore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class TierSeasonManager {

    private final JavaPlugin plugin;
    private final TierSpaceStore store;
    private final ModeRegistry modeRegistry;

    private int seasonNumber;
    private long seasonEndMillis;
    private int resetBase;
    private double resetFactor;

    public TierSeasonManager(JavaPlugin plugin, TierSpaceStore store, ModeRegistry modeRegistry) {
        this.plugin = plugin;
        this.store = store;
        this.modeRegistry = modeRegistry;
    }

    public void reload(FileConfiguration config) {
        seasonNumber = config.getInt("season.number", 1);
        resetBase = config.getInt("season.reset-base", TierRankUtil.defaultRating());
        resetFactor = config.getDouble("season.reset-factor", 0.3);

        int durationDays = config.getInt("season.duration-days", 45);
        String startDateRaw = config.getString("season.start-date", LocalDate.now().toString());
        LocalDate startDate = LocalDate.parse(startDateRaw);
        LocalDate endDate = startDate.plusDays(durationDays);
        seasonEndMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        int storedSeason = store.getGlobalSeasonNumber();
        long storedEnd = store.getGlobalSeasonEndMillis();
        if (storedSeason > 0) {
            seasonNumber = Math.max(seasonNumber, storedSeason);
        }
        if (storedEnd > 0) {
            seasonEndMillis = storedEnd;
        } else {
            store.saveGlobalSeason(seasonNumber, seasonEndMillis);
        }
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkSeasonEnd, 20L * 60, 20L * 60 * 30);
    }

    public void ensurePlayerSeason(UUID uuid) {
        int playerSeason = store.getPlayerSeason(uuid);
        if (playerSeason < seasonNumber) {
            applySeasonReset(uuid);
            store.setPlayerSeason(uuid, seasonNumber);
        }
    }

    public void ensurePlayerSeason(Player player) {
        ensurePlayerSeason(player.getUniqueId());
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public long getDaysRemaining() {
        long millisLeft = seasonEndMillis - System.currentTimeMillis();
        if (millisLeft <= 0) {
            return 0;
        }
        return ChronoUnit.DAYS.between(Instant.now(), Instant.ofEpochMilli(seasonEndMillis));
    }

    public String getSeasonDisplayLine() {
        long days = getDaysRemaining();
        if (days <= 0) {
            return "§7Season " + seasonNumber + " §8| §cEnding soon";
        }
        return "§7Season " + seasonNumber + " §8| §eEnds in: §f" + days + " days";
    }

    private void checkSeasonEnd() {
        if (System.currentTimeMillis() < seasonEndMillis) {
            return;
        }

        seasonNumber++;
        plugin.getConfig().set("season.number", seasonNumber);
        plugin.getConfig().set("season.start-date", LocalDate.now().toString());
        plugin.saveConfig();
        reload(plugin.getConfig());
        store.saveGlobalSeason(seasonNumber, seasonEndMillis);

        for (Player player : Bukkit.getOnlinePlayers()) {
            applySeasonReset(player.getUniqueId());
            store.setPlayerSeason(player.getUniqueId(), seasonNumber);
            player.sendMessage("§6§lNEW SEASON §7→ Season " + seasonNumber + " começou!");
            player.sendMessage("§7Ratings foram suavizados: §f" + resetBase + " + (old × " + resetFactor + ")");
        }

        plugin.getLogger().info("TierSpace season rolled to " + seasonNumber);
    }

    private void applySeasonReset(UUID uuid) {
        for (GameModeId mode : modeRegistry.enabledModes()) {
            store.ensureInitialized(uuid, mode);
            int oldRating = store.getRating(uuid, mode);
            int newRating = resetBase + (int) Math.round(oldRating * resetFactor);
            store.setRating(uuid, mode, newRating);
            store.resetPlacement(uuid, mode);
        }
    }
}

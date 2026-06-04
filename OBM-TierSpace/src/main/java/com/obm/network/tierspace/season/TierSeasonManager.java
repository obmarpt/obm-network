package com.obm.network.tierspace.season;

import com.obm.network.core.tier.TierRankUtil;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.storage.TierSpaceStore;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Temporadas TierSpace — apenas mudança manual ({@link #startNewSeason(boolean, CommandSender)}).
 */
public class TierSeasonManager {

    /** Nunca activar auto-season por código (só manual). */
    private static final boolean AUTO_SEASON = false;

    private final JavaPlugin plugin;
    private final TierSpaceStore store;
    private final ModeRegistry modeRegistry;

    private int seasonNumber;
    private long seasonEndMillis;
    private int resetBase;
    private double resetFactor;
    private int durationDays;

    public TierSeasonManager(JavaPlugin plugin, TierSpaceStore store, ModeRegistry modeRegistry) {
        this.plugin = plugin;
        this.store = store;
        this.modeRegistry = modeRegistry;
    }

    public void reload(FileConfiguration config) {
        resetBase = config.getInt("season.reset-base", TierRankUtil.defaultRating());
        resetFactor = config.getDouble("season.reset-factor", 0.3);
        durationDays = Math.max(1, config.getInt("season.duration-days", 45));

        if (store.isSeasonPersisted()) {
            seasonNumber = Math.max(1, store.getGlobalSeasonNumber());
            seasonEndMillis = store.getGlobalSeasonEndMillis();
            if (seasonEndMillis <= 0) {
                seasonEndMillis = computeEndMillisFromConfig(config);
                store.saveGlobalSeason(seasonNumber, seasonEndMillis, durationDays);
            }
            syncConfigSeasonNumber();
            return;
        }

        seasonNumber = Math.max(1, config.getInt("season.number", 1));
        seasonEndMillis = computeEndMillisFromConfig(config);
        store.saveGlobalSeason(seasonNumber, seasonEndMillis, durationDays);
        syncConfigSeasonNumber();
        plugin.getLogger().info("TierSpace season inicializada: " + seasonNumber + " (manual-only)");
    }

    /** Sem timer — auto-season desactivado. */
    public void start() {
        // Intencionalmente vazio: não agendar checkSeasonEnd / season++
    }

    public void stop() {
        // Sem task activa
    }

    public void ensurePlayerSeason(UUID uuid) {
        int playerSeason = store.getPlayerSeason(uuid);
        if (playerSeason <= 0) {
            store.setPlayerSeason(uuid, seasonNumber);
            return;
        }
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

    public boolean isAutoSeason() {
        return AUTO_SEASON;
    }

    public String getSeasonDisplayLine() {
        return "§7Season " + seasonNumber + " §8| §7Controlo manual";
    }

    /**
     * Inicia nova temporada — só quando {@code manual == true}.
     */
    public boolean startNewSeason(boolean manual, CommandSender initiator) {
        if (!manual) {
            return false;
        }
        int next = seasonNumber + 1;
        applyNewSeason(next, true, initiator);
        return true;
    }

    public boolean advanceSeasonManually(CommandSender initiator) {
        return startNewSeason(true, initiator);
    }

    public boolean setSeasonNumberManually(int target, CommandSender initiator) {
        if (target < 0) {
            if (initiator != null) {
                initiator.sendMessage("§cSeason inválida. Usa §f/season ranked reset 0 confirm§c.");
            }
            return false;
        }
        if (target == 0) {
            return resetToSeasonZero(initiator);
        }
        seasonNumber = target;
        seasonEndMillis = LocalDate.now().plusDays(durationDays)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        store.saveGlobalSeason(seasonNumber, seasonEndMillis, durationDays);
        syncConfigSeasonNumber();
        plugin.getConfig().set("season.start-date", LocalDate.now().toString());
        plugin.saveConfig();
        if (initiator != null) {
            initiator.sendMessage("§aSeason definida para §f" + seasonNumber + "§a (sem reset de ratings).");
        }
        plugin.getLogger().info("TierSpace season set manually to " + seasonNumber);
        return true;
    }

    /** Anuncia fim da season actual sem incrementar nem resetar. */
    public void endSeason(CommandSender initiator) {
        String msg = "§6§lSEASON END §7→ A temporada §f" + seasonNumber + " §7terminou.";
        Bukkit.broadcastMessage(msg);
        if (initiator != null) {
            initiator.sendMessage("§aAnúncio de fim de season enviado.");
        }
    }

    /**
     * Season 0 — reset completo de dados ranked (ELO/stats por modo).
     * Não altera Emeralds, cosmetics nem ranks da network.
     */
    public boolean resetToSeasonZero(CommandSender initiator) {
        seasonNumber = 0;
        seasonEndMillis = System.currentTimeMillis();
        saveSeason();

        ConfigurationSection players = playersSection();
        int count = 0;
        if (players != null) {
            for (String key : players.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    hardResetPlayer(uuid);
                    store.setPlayerSeason(uuid, 0);
                    count++;
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        String log = "[OBM] TierSpace season reset to 0 — " + count + " players hard-reset";
        plugin.getLogger().warning(log);
        Bukkit.broadcastMessage("§c§l⚠ §7TierSpace reposto para §fSeason #0§7 (dados ranked resetados).");
        if (initiator != null) {
            initiator.sendMessage("§aSeason Ranked §f#0§a. §7Jogadores afectados: §f" + count);
            initiator.sendMessage("§7Emeralds, cosmetics e ranks network §anão§7 foram alterados.");
        }
        return true;
    }

    private void hardResetPlayer(UUID uuid) {
        for (GameModeId mode : modeRegistry.enabledModes()) {
            store.resetModeProgress(uuid, mode, resetBase);
        }
    }

    /** Reset suave de ratings de todos os jogadores (sem mudar número da season). */
    public int resetAllRatings(CommandSender initiator) {
        int count = 0;
        ConfigurationSection players = playersSection();
        if (players == null) {
            if (initiator != null) {
                initiator.sendMessage("§cNenhum jogador na base de dados.");
            }
            return 0;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                applySeasonReset(uuid);
                store.setPlayerSeason(uuid, seasonNumber);
                count++;
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (initiator != null) {
            initiator.sendMessage("§aRatings resetados para §f" + count + " §ajogadores.");
        }
        plugin.getLogger().info("TierSpace manual rating reset: " + count + " players");
        return count;
    }

    private void applyNewSeason(int newSeasonNumber, boolean notifyPlayers, CommandSender initiator) {
        seasonNumber = newSeasonNumber;
        seasonEndMillis = LocalDate.now().plusDays(durationDays)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        saveSeason();
        syncConfigSeasonNumber();
        plugin.getConfig().set("season.start-date", LocalDate.now().toString());
        plugin.saveConfig();

        ConfigurationSection players = playersSection();
        if (players != null) {
            for (String key : players.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    applySeasonReset(uuid);
                    store.setPlayerSeason(uuid, seasonNumber);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (notifyPlayers) {
            String broadcast = "§6§lNEW SEASON §7→ Season " + seasonNumber + " começou!";
            Bukkit.broadcastMessage(broadcast);
            Bukkit.broadcastMessage("§7Ratings suavizados: §f" + resetBase + " + (old × " + resetFactor + ")");
        }

        if (initiator != null) {
            initiator.sendMessage("§aNova season: §f" + seasonNumber);
        }
        plugin.getLogger().info("TierSpace season manually advanced to " + seasonNumber);
    }

    private void saveSeason() {
        store.saveGlobalSeason(seasonNumber, seasonEndMillis, durationDays);
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

    private long computeEndMillisFromConfig(FileConfiguration config) {
        String startDateRaw = config.getString("season.start-date", LocalDate.now().toString());
        LocalDate startDate = LocalDate.parse(startDateRaw);
        return startDate.plusDays(durationDays).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void syncConfigSeasonNumber() {
        if (plugin.getConfig().getInt("season.number", 1) != seasonNumber) {
            plugin.getConfig().set("season.number", seasonNumber);
            plugin.saveConfig();
        }
    }

    private ConfigurationSection playersSection() {
        var core = com.obm.network.core.OBMCorePlugin.get();
        if (core == null || core.getDataStore() == null) {
            return null;
        }
        return core.getDataStore().getYaml().getConfigurationSection("players");
    }
}

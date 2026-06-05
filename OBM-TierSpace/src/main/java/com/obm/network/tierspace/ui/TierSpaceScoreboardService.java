package com.obm.network.tierspace.ui;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.progression.DailyQuestService;
import com.obm.network.tierspace.progression.PlacementService;
import com.obm.network.tierspace.storage.TierSpaceStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Delega scoreboard ao OBM-Core ({@link com.obm.network.core.scoreboard.ScoreboardManager}).
 */
public class TierSpaceScoreboardService {

    private final JavaPlugin plugin;
    private final Map<UUID, GameModeId> activeModes = new ConcurrentHashMap<>();
    private BukkitTask refreshTask;

    public TierSpaceScoreboardService(JavaPlugin plugin,
                                      TierSpaceStore store,
                                      PlacementService placementService,
                                      DailyQuestService dailyQuestService,
                                      ModeRegistry modeRegistry) {
        this.plugin = plugin;
    }

    public void start() {
        if (refreshTask != null) {
            return;
        }
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 40L, 40L);
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        activeModes.clear();
    }

    public void track(Player player, GameModeId mode) {
        activeModes.put(player.getUniqueId(), mode);
        refreshPlayer(player);
    }

    public void untrack(UUID uuid) {
        activeModes.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            refreshPlayer(player);
        }
    }

    public void updatePlayer(Player player, GameModeId mode) {
        activeModes.put(player.getUniqueId(), mode);
        refreshPlayer(player);
    }

    private void refreshAll() {
        for (UUID uuid : activeModes.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                refreshPlayer(player);
            }
        }
    }

    private void refreshPlayer(Player player) {
        OBMCorePlugin core = OBMCorePlugin.get();
        if (core != null && core.getScoreboardManager() != null) {
            core.getScoreboardManager().updateScoreboard(player);
        }
    }
}

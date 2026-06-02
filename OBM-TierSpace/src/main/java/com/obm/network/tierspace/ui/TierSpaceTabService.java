package com.obm.network.tierspace.ui;

import com.obm.network.core.tier.TierRankUtil;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.progression.DailyQuestService;
import com.obm.network.tierspace.progression.PlacementService;
import com.obm.network.tierspace.reward.RankRewardService;
import com.obm.network.tierspace.storage.TierSpaceStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TierSpaceTabService {

    private final JavaPlugin plugin;
    private final TierSpaceStore store;
    private final PlacementService placementService;
    private final RankRewardService rankRewardService;
    private BukkitTask refreshTask;

    public TierSpaceTabService(JavaPlugin plugin,
                               TierSpaceStore store,
                               PlacementService placementService,
                               RankRewardService rankRewardService) {
        this.plugin = plugin;
        this.store = store;
        this.placementService = placementService;
        this.rankRewardService = rankRewardService;
    }

    public void start() {
        if (refreshTask != null) {
            return;
        }
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 20L, 40L);
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshAll, 5L);
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    public void refresh(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        String prefix = rankRewardService.getRewardPrefix(player.getUniqueId());
        player.setPlayerListName(prefix + buildTabPrefix(player) + " §f" + player.getName());
    }

    public String buildTabPrefix(Player player) {
        return buildTabPrefix(player.getUniqueId(), GameModeId.SWORD);
    }

    public String buildTabPrefix(java.util.UUID uuid, GameModeId mode) {
        if (placementService.isInPlacement(uuid, mode)) {
            return "§7[§e" + placementService.getPlacementLabel(uuid, mode) + "§7]";
        }
        return "§7[" + TierRankUtil.fromRating(store.getRating(uuid, mode)).displayName() + "§7]";
    }

    private void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }
}

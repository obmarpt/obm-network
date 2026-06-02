package com.obm.network.tierspace.ui;

import com.obm.network.core.tier.TierRankUtil;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.progression.DailyQuestService;
import com.obm.network.tierspace.progression.PlacementService;
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

public class TierSpaceScoreboardService {

    private final JavaPlugin plugin;
    private final TierSpaceStore store;
    private final PlacementService placementService;
    private final DailyQuestService dailyQuestService;
    private final ModeRegistry modeRegistry;
    private final Map<UUID, GameModeId> activeModes = new ConcurrentHashMap<>();
    private BukkitTask refreshTask;

    public TierSpaceScoreboardService(JavaPlugin plugin,
                                      TierSpaceStore store,
                                      PlacementService placementService,
                                      DailyQuestService dailyQuestService,
                                      ModeRegistry modeRegistry) {
        this.plugin = plugin;
        this.store = store;
        this.placementService = placementService;
        this.dailyQuestService = dailyQuestService;
        this.modeRegistry = modeRegistry;
    }

    public void start() {
        if (refreshTask != null) {
            return;
        }
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 20L, 20L);
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
        updatePlayer(player, mode);
    }

    public void untrack(UUID uuid) {
        activeModes.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void refreshAll() {
        for (Map.Entry<UUID, GameModeId> entry : activeModes.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                updatePlayer(player, entry.getValue());
            }
        }
    }

    public void updatePlayer(Player player, GameModeId mode) {
        UUID uuid = player.getUniqueId();
        store.ensureInitialized(uuid, mode);

        int rating = store.getRating(uuid, mode);
        int wins = store.getWins(uuid, mode);
        int losses = store.getLosses(uuid, mode);
        int streak = store.getStreak(uuid, mode);
        boolean inPlacement = placementService.isInPlacement(uuid, mode);

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("tierspace", "dummy",
                ChatColor.AQUA + "" + ChatColor.BOLD + "TierSpace");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        setLine(objective, ChatColor.GRAY + "Mode: " + ChatColor.WHITE + modeRegistry.displayName(mode), 10);
        if (inPlacement) {
            setLine(objective, ChatColor.YELLOW + placementService.getPlacementLabel(uuid, mode), 9);
        } else {
            var rank = TierRankUtil.fromRating(rating);
            setLine(objective, ChatColor.GRAY + "Rank: " + rank.displayName(), 9);
        }
        setLine(objective, ChatColor.GRAY + "Rating: " + ChatColor.WHITE + rating, 8);
        if (!inPlacement) {
            setLine(objective, TierRankUtil.formatNextRankLine(rating), 7);
            setLine(objective, ChatColor.GRAY + "Progress: " + ChatColor.AQUA + TierRankUtil.formatProgress(rating), 6);
        }
        setLine(objective, ChatColor.GRAY + "W/L: " + ChatColor.GREEN + wins + ChatColor.GRAY + " / " + ChatColor.RED + losses, 5);
        setLine(objective, ChatColor.GRAY + "Streak: " + ChatColor.YELLOW + streak, 4);
        setLine(objective, dailyQuestService.getProgressLine(uuid, mode), 3);
        setLine(objective, ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "----------------", 2);

        player.setScoreboard(board);
    }

    private void setLine(Objective objective, String text, int score) {
        objective.getScore(text).setScore(score);
    }
}

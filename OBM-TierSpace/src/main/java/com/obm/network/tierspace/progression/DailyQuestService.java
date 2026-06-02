package com.obm.network.tierspace.progression;

import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.storage.TierSpaceStore;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DailyQuestService {

    private final TierSpaceStore store;
    private final int winsRequired;
    private final int winsRatingReward;
    private final int matchesRequired;
    private final int streakRequired;

    public DailyQuestService(TierSpaceStore store, int winsRequired, int winsRatingReward,
                             int matchesRequired, int streakRequired) {
        this.store = store;
        this.winsRequired = winsRequired;
        this.winsRatingReward = winsRatingReward;
        this.matchesRequired = matchesRequired;
        this.streakRequired = streakRequired;
    }

    public void recordMatch(UUID uuid, GameModeId mode) {
        store.ensureDailyReset(uuid, mode);
        store.incrementDailyMatches(uuid, mode);
        checkMatchQuest(uuid, mode);
    }

    public void recordWin(UUID uuid, GameModeId mode, int newStreak) {
        store.ensureDailyReset(uuid, mode);
        store.incrementDailyWins(uuid, mode);
        checkWinQuest(uuid, mode);
        checkStreakQuest(uuid, mode, newStreak);
    }

    private void checkWinQuest(UUID uuid, GameModeId mode) {
        if (store.isDailyQuestWinsClaimed(uuid, mode)) {
            return;
        }
        if (store.getDailyWins(uuid, mode) < winsRequired) {
            return;
        }

        store.setDailyQuestWinsClaimed(uuid, mode, true);
        int newRating = store.getRating(uuid, mode) + winsRatingReward;
        store.setRating(uuid, mode, newRating);

        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage("§a§lDAILY QUEST §7→ Vence " + winsRequired + " duelos");
            player.sendMessage("§7+§e" + winsRatingReward + " rating §7(§f" + newRating + "§7)");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        }
    }

    private void checkStreakQuest(UUID uuid, GameModeId mode, int streak) {
        if (store.isDailyQuestStreakClaimed(uuid, mode)) {
            return;
        }
        if (streak < streakRequired) {
            return;
        }

        store.setDailyQuestStreakClaimed(uuid, mode, true);
        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage("§e§lDAILY QUEST §7→ Atinge streak " + streakRequired);
            player.sendMessage("§7Título desbloqueado: §eStreak Hunter");
            player.sendTitle("§eStreak Hunter", "§7Daily quest completa", 5, 40, 10);
        }
    }

    private void checkMatchQuest(UUID uuid, GameModeId mode) {
        if (store.isDailyQuestMatchesClaimed(uuid, mode)) {
            return;
        }
        if (store.getDailyMatches(uuid, mode) < matchesRequired) {
            return;
        }

        store.setDailyQuestMatchesClaimed(uuid, mode, true);
        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage("§b§lDAILY QUEST §7→ Joga " + matchesRequired + " matches");
            player.sendMessage("§7Badge: §bTierSpace Grinder");
        }
    }

    public String getProgressLine(UUID uuid, GameModeId mode) {
        store.ensureDailyReset(uuid, mode);
        return "§7Quests: §a" + store.getDailyWins(uuid, mode) + "/" + winsRequired + " wins §7| §b"
                + store.getDailyMatches(uuid, mode) + "/" + matchesRequired + " games";
    }
}

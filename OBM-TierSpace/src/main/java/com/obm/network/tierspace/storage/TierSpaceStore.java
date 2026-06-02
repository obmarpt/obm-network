package com.obm.network.tierspace.storage;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import com.obm.network.tierspace.mode.GameModeId;

import java.time.LocalDate;
import java.util.UUID;

public class TierSpaceStore {

    private final DataStore dataStore;
    private final int defaultRating;

    public TierSpaceStore(int defaultRating) {
        this.dataStore = OBMCorePlugin.get().getDataStore();
        this.defaultRating = defaultRating;
    }

    public void ensureInitialized(UUID uuid, GameModeId mode) {
        if (!dataStore.has(uuid, key(mode, "rating"))) {
            dataStore.set(uuid, key(mode, "rating"), defaultRating);
        }
        if (!dataStore.has(uuid, key(mode, "placed"))) {
            dataStore.setBoolean(uuid, key(mode, "placed"), false);
        }
        if (!dataStore.has(uuid, key(mode, "placement_matches"))) {
            dataStore.set(uuid, key(mode, "placement_matches"), 0);
        }
        dataStore.save(uuid);
    }

    public int getRating(UUID uuid, GameModeId mode) {
        ensureInitialized(uuid, mode);
        int rating = dataStore.getInt(uuid, key(mode, "rating"));
        return rating > 0 ? rating : defaultRating;
    }

    public void setRating(UUID uuid, GameModeId mode, int rating) {
        dataStore.set(uuid, key(mode, "rating"), Math.max(0, rating));
        dataStore.save(uuid);
    }

    public int getWins(UUID uuid, GameModeId mode) {
        return dataStore.getInt(uuid, key(mode, "wins"));
    }

    public int getLosses(UUID uuid, GameModeId mode) {
        return dataStore.getInt(uuid, key(mode, "losses"));
    }

    public int getStreak(UUID uuid, GameModeId mode) {
        return dataStore.getInt(uuid, key(mode, "streak"));
    }

    public int getBestStreak(UUID uuid, GameModeId mode) {
        return dataStore.getInt(uuid, key(mode, "best_streak"));
    }

    public int getRatingLossStreak(UUID uuid, GameModeId mode) {
        return dataStore.getInt(uuid, key(mode, "loss_streak"));
    }

    public void setRatingLossStreak(UUID uuid, GameModeId mode, int streak) {
        dataStore.set(uuid, key(mode, "loss_streak"), Math.max(0, streak));
        dataStore.save(uuid);
    }

    public int getPlacementMatches(UUID uuid, GameModeId mode) {
        return dataStore.getInt(uuid, key(mode, "placement_matches"));
    }

    public void setPlacementMatches(UUID uuid, GameModeId mode, int count) {
        dataStore.set(uuid, key(mode, "placement_matches"), Math.max(0, count));
        dataStore.save(uuid);
    }

    public boolean isPlaced(UUID uuid, GameModeId mode) {
        return dataStore.getBoolean(uuid, key(mode, "placed"));
    }

    public void setPlaced(UUID uuid, GameModeId mode, boolean placed) {
        dataStore.setBoolean(uuid, key(mode, "placed"), placed);
        dataStore.save(uuid);
    }

    public int getGamesPlayed(UUID uuid, GameModeId mode) {
        return getWins(uuid, mode) + getLosses(uuid, mode);
    }

    public boolean hasRating(UUID uuid, GameModeId mode) {
        return dataStore.has(uuid, key(mode, "rating"));
    }

    public int getPlayerSeason(UUID uuid) {
        return dataStore.getInt(uuid, "tierspace_season");
    }

    public void setPlayerSeason(UUID uuid, int season) {
        dataStore.set(uuid, "tierspace_season", season);
        dataStore.save(uuid);
    }

    public void resetPlacement(UUID uuid, GameModeId mode) {
        dataStore.set(uuid, key(mode, "placement_matches"), 0);
        dataStore.setBoolean(uuid, key(mode, "placed"), false);
        dataStore.save(uuid);
    }

    public void setRewardTier(UUID uuid, String tierName) {
        dataStore.set(uuid, "tierspace_reward_tier", tierName == null ? "" : tierName.toLowerCase());
        dataStore.save(uuid);
    }

    public String getRewardTier(UUID uuid) {
        Object value = dataStore.getYaml().get("players." + uuid + ".tierspace_reward_tier");
        return value == null ? "" : value.toString();
    }

    public void saveGlobalSeason(int seasonNumber, long seasonEndMillis) {
        dataStore.getYaml().set("tierspace.season.number", seasonNumber);
        dataStore.getYaml().set("tierspace.season.end", seasonEndMillis);
        dataStore.save();
    }

    public int getGlobalSeasonNumber() {
        return dataStore.getYaml().getInt("tierspace.season.number", 1);
    }

    public long getGlobalSeasonEndMillis() {
        return dataStore.getYaml().getLong("tierspace.season.end", 0L);
    }

    public boolean isDailyWinAvailable(UUID uuid, GameModeId mode) {
        return !LocalDate.now().toString().equals(getDailyWinDate(uuid, mode));
    }

    public boolean claimDailyWin(UUID uuid, GameModeId mode) {
        if (!isDailyWinAvailable(uuid, mode)) {
            return false;
        }
        dataStore.set(uuid, key(mode, "daily_win"), LocalDate.now().toString());
        dataStore.save(uuid);
        return true;
    }

    public String getDailyWinDate(UUID uuid, GameModeId mode) {
        Object value = dataStore.getYaml().get("players." + uuid + "." + key(mode, "daily_win"));
        return value == null ? "" : value.toString();
    }

    public void ensureDailyReset(UUID uuid, GameModeId mode) {
        String today = LocalDate.now().toString();
        String stored = getDailyQuestDate(uuid, mode);
        if (today.equals(stored)) {
            return;
        }
        dataStore.set(uuid, key(mode, "daily_date"), today);
        dataStore.set(uuid, key(mode, "daily_wins"), 0);
        dataStore.set(uuid, key(mode, "daily_matches"), 0);
        dataStore.setBoolean(uuid, key(mode, "daily_quest_wins"), false);
        dataStore.setBoolean(uuid, key(mode, "daily_quest_streak"), false);
        dataStore.setBoolean(uuid, key(mode, "daily_quest_matches"), false);
        dataStore.save(uuid);
    }

    public String getDailyQuestDate(UUID uuid, GameModeId mode) {
        Object value = dataStore.getYaml().get("players." + uuid + "." + key(mode, "daily_date"));
        return value == null ? "" : value.toString();
    }

    public int getDailyWins(UUID uuid, GameModeId mode) {
        ensureDailyReset(uuid, mode);
        return dataStore.getInt(uuid, key(mode, "daily_wins"));
    }

    public int getDailyMatches(UUID uuid, GameModeId mode) {
        ensureDailyReset(uuid, mode);
        return dataStore.getInt(uuid, key(mode, "daily_matches"));
    }

    public void incrementDailyWins(UUID uuid, GameModeId mode) {
        dataStore.set(uuid, key(mode, "daily_wins"), getDailyWins(uuid, mode) + 1);
        dataStore.save(uuid);
    }

    public void incrementDailyMatches(UUID uuid, GameModeId mode) {
        dataStore.set(uuid, key(mode, "daily_matches"), getDailyMatches(uuid, mode) + 1);
        dataStore.save(uuid);
    }

    public boolean isDailyQuestWinsClaimed(UUID uuid, GameModeId mode) {
        ensureDailyReset(uuid, mode);
        return dataStore.getBoolean(uuid, key(mode, "daily_quest_wins"));
    }

    public void setDailyQuestWinsClaimed(UUID uuid, GameModeId mode, boolean claimed) {
        dataStore.setBoolean(uuid, key(mode, "daily_quest_wins"), claimed);
        dataStore.save(uuid);
    }

    public boolean isDailyQuestStreakClaimed(UUID uuid, GameModeId mode) {
        ensureDailyReset(uuid, mode);
        return dataStore.getBoolean(uuid, key(mode, "daily_quest_streak"));
    }

    public void setDailyQuestStreakClaimed(UUID uuid, GameModeId mode, boolean claimed) {
        dataStore.setBoolean(uuid, key(mode, "daily_quest_streak"), claimed);
        dataStore.save(uuid);
    }

    public boolean isDailyQuestMatchesClaimed(UUID uuid, GameModeId mode) {
        ensureDailyReset(uuid, mode);
        return dataStore.getBoolean(uuid, key(mode, "daily_quest_matches"));
    }

    public void setDailyQuestMatchesClaimed(UUID uuid, GameModeId mode, boolean claimed) {
        dataStore.setBoolean(uuid, key(mode, "daily_quest_matches"), claimed);
        dataStore.save(uuid);
    }

    public void recordWin(UUID uuid, GameModeId mode, int newRating, int newStreak) {
        dataStore.set(uuid, key(mode, "rating"), Math.max(0, newRating));
        dataStore.increment(uuid, key(mode, "wins"));
        dataStore.set(uuid, key(mode, "streak"), newStreak);
        dataStore.set(uuid, key(mode, "loss_streak"), 0);
        if (newStreak > getBestStreak(uuid, mode)) {
            dataStore.set(uuid, key(mode, "best_streak"), newStreak);
        }
        dataStore.save(uuid);
    }

    public void recordLoss(UUID uuid, GameModeId mode, int newRating, int ratingLossStreak) {
        dataStore.set(uuid, key(mode, "rating"), Math.max(0, newRating));
        dataStore.increment(uuid, key(mode, "losses"));
        dataStore.set(uuid, key(mode, "streak"), 0);
        dataStore.set(uuid, key(mode, "loss_streak"), ratingLossStreak);
        dataStore.save(uuid);
    }

    public String statKey(GameModeId mode, String stat) {
        return key(mode, stat);
    }

    private String key(GameModeId mode, String stat) {
        return "tierspace_" + mode.id() + "_" + stat;
    }
}

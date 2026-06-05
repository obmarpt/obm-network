package com.obm.network.smp.progression;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import com.obm.network.core.storage.PlayerStatsKeys;

import java.util.UUID;

public class PlayerProgressionStore {

    private static final String RANK_KEY = PlayerStatsKeys.SMP_RANK;
    private static final String XP_KEY = PlayerStatsKeys.SMP_XP;
    private static final String LEVEL_KEY = PlayerStatsKeys.SMP_LEVEL;

    private final DataStore dataStore;

    public PlayerProgressionStore() {
        this.dataStore = OBMCorePlugin.get().getDataStore();
    }

    public String getRankId(UUID uuid, String defaultRank) {
        if (!dataStore.has(uuid, RANK_KEY)) {
            return defaultRank;
        }
        Object value = dataStore.getYaml().get("players." + uuid + "." + RANK_KEY);
        return value != null ? value.toString() : defaultRank;
    }

    public void setRankId(UUID uuid, String rankId) {
        dataStore.set(uuid, RANK_KEY, rankId);
        dataStore.save(uuid);
    }

    public int getXp(UUID uuid) {
        return dataStore.getInt(uuid, XP_KEY);
    }

    public void setXp(UUID uuid, int xp) {
        dataStore.set(uuid, XP_KEY, Math.max(0, xp));
        dataStore.save(uuid);
    }

    public int getLevel(UUID uuid) {
        int level = dataStore.getInt(uuid, LEVEL_KEY);
        return Math.max(1, level <= 0 ? 1 : level);
    }

    public void setLevel(UUID uuid, int level) {
        dataStore.set(uuid, LEVEL_KEY, Math.max(1, level));
        dataStore.save(uuid);
    }

    public void ensureInitialized(UUID uuid, String defaultRank) {
        if (!dataStore.has(uuid, RANK_KEY)) {
            setRankId(uuid, defaultRank);
        }
        if (!dataStore.has(uuid, LEVEL_KEY)) {
            setLevel(uuid, 1);
        }
        if (!dataStore.has(uuid, XP_KEY)) {
            setXp(uuid, 0);
        }
    }
}

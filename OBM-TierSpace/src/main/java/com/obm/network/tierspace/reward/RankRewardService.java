package com.obm.network.tierspace.reward;

import com.obm.network.core.tier.TierRank;
import com.obm.network.core.tier.TierRankUtil;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.storage.TierSpaceStore;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RankRewardService {

    private final JavaPlugin plugin;
    private final TierSpaceStore store;
    private final ModeRegistry modeRegistry;
    private final Map<String, RankReward> rewards = new HashMap<>();
    private BukkitTask particleTask;

    public RankRewardService(JavaPlugin plugin, TierSpaceStore store, ModeRegistry modeRegistry) {
        this.plugin = plugin;
        this.store = store;
        this.modeRegistry = modeRegistry;
    }

    public void reload(FileConfiguration config) {
        rewards.clear();
        ConfigurationSection section = config.getConfigurationSection("rank-rewards");
        if (section == null) {
            return;
        }
        for (String tierName : section.getKeys(false)) {
            ConfigurationSection rewardSection = section.getConfigurationSection(tierName);
            if (rewardSection == null) {
                continue;
            }
            String prefix = rewardSection.getString("prefix", "");
            String tag = rewardSection.getString("tag", "");
            String particleName = rewardSection.getString("particle");
            Particle particle = null;
            if (particleName != null) {
                try {
                    particle = Particle.valueOf(particleName.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            rewards.put(tierName.toLowerCase(), new RankReward(prefix, tag, particle));
        }
    }

    public void start() {
        if (particleTask != null) {
            return;
        }
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnParticles, 20L, 40L);
    }

    public void stop() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }

    public void refresh(Player player) {
        TierRank bestRank = getBestRank(player.getUniqueId());
        store.setRewardTier(player.getUniqueId(), bestRank.tierName().toLowerCase());
    }

    public String getRewardPrefix(UUID uuid) {
        TierRank rank = getBestRank(uuid);
        RankReward reward = getReward(rank.tierName());
        return reward == null ? "" : reward.prefix();
    }

    public TierRank getBestRank(UUID uuid) {
        TierRank best = TierRankUtil.fromRating(TierRankUtil.defaultRating());
        for (GameModeId mode : modeRegistry.enabledModes()) {
            if (store.getGamesPlayed(uuid, mode) <= 0 && !store.hasRating(uuid, mode)) {
                continue;
            }
            TierRank rank = TierRankUtil.fromRating(store.getRating(uuid, mode));
            if (TierRankUtil.compare(rank, best) > 0) {
                best = rank;
            }
        }
        return best;
    }

    private RankReward getReward(String tierName) {
        if (tierName == null) {
            return null;
        }
        return rewards.get(tierName.toLowerCase());
    }

    private void spawnParticles() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            TierRank rank = getBestRank(player.getUniqueId());
            RankReward reward = getReward(rank.tierName());
            if (reward == null || reward.particle() == null) {
                continue;
            }
            player.getWorld().spawnParticle(
                    reward.particle(),
                    player.getLocation().add(0, 1.0, 0),
                    3,
                    0.3, 0.4, 0.3,
                    0.01
            );
        }
    }

    public record RankReward(String prefix, String tag, Particle particle) {
    }
}

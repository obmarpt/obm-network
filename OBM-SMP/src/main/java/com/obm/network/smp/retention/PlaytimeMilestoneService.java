package com.obm.network.smp.retention;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.SMPPlugin;
import com.obm.network.smp.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Recompensas únicas por marcos de playtime (usa playtime_smp do DataStore).
 */
public class PlaytimeMilestoneService {

    private final SMPPlugin plugin;
    private final EconomyService economy;
    private final WorldModeService worldModeService;

    public PlaytimeMilestoneService(SMPPlugin plugin, EconomyService economy, WorldModeService worldModeService) {
        this.plugin = plugin;
        this.economy = economy;
        this.worldModeService = worldModeService;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("rewards.milestones.enabled", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskTimer(plugin, this::checkOnlinePlayers, 20L * 60L, 20L * 60L);
    }

    private void checkOnlinePlayers() {
        DataStore ds = OBMCorePlugin.get().getDataStore();
        List<?> tiers = plugin.getConfig().getMapList("rewards.milestones.tiers");
        if (tiers.isEmpty()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!worldModeService.isSMP(player.getWorld().getName())) {
                continue;
            }

            UUID uuid = player.getUniqueId();
            int playtimeSeconds = ds.getInt(uuid, "playtime_smp");

            for (Object raw : tiers) {
                if (!(raw instanceof java.util.Map<?, ?> map)) {
                    continue;
                }
                int minutes = parseInt(map.get("minutes"), 0);
                int coins = parseInt(map.get("coins"), 0);
                if (minutes <= 0 || coins <= 0) {
                    continue;
                }

                int requiredSeconds = minutes * 60;
                String claimKey = "playtime_milestone_" + minutes;

                if (playtimeSeconds < requiredSeconds || ds.getBoolean(uuid, claimKey)) {
                    continue;
                }

                var deposit = economy.deposit(uuid, coins);
                if (!deposit.transactionSuccess()) {
                    continue;
                }
                ds.setBoolean(uuid, claimKey, true);
                ds.save(uuid);
                RetentionFeedback.playtimeMilestone(player, minutes, coins);
            }
        }
    }

    private static int parseInt(Object value, int def) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

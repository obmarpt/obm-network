package com.obm.network.tierspace.hub;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.world.WorldModeService;
import org.bukkit.entity.Player;

public final class TierSpaceHub {

    private TierSpaceHub() {
    }

    public static boolean isInTierSpaceHub(Player player) {
        if (player == null) {
            return false;
        }
        return isInTierSpaceHub(player.getWorld().getName());
    }

    public static boolean isInTierSpaceHub(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        OBMCorePlugin core = OBMCorePlugin.get();
        if (core == null) {
            return "rankedSpawn".equalsIgnoreCase(worldName);
        }
        WorldModeService worlds = core.getWorldModeService();
        return worlds != null && worlds.isRanked(worldName);
    }
}

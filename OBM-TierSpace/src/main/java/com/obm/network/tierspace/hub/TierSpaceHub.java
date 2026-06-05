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
        OBMCorePlugin core = OBMCorePlugin.get();
        if (core == null) {
            return false;
        }
        WorldModeService worlds = core.getWorldModeService();
        return worlds != null && worlds.isRanked(player.getWorld().getName());
    }
}

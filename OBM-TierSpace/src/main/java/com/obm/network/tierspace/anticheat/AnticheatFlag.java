package com.obm.network.tierspace.anticheat;

import org.bukkit.entity.Player;

public record AnticheatFlag(
        String source,
        Player player,
        String check,
        double vl,
        String verbose
) {
}

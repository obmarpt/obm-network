package com.obm.network.smp.duel;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class DuelReturnPoint {

    private final Location location;

    private DuelReturnPoint(Location location) {
        this.location = location == null ? null : location.clone();
    }

    public static DuelReturnPoint capture(Player player) {
        return new DuelReturnPoint(player.getLocation());
    }

    public Location location() {
        return location == null ? null : location.clone();
    }
}

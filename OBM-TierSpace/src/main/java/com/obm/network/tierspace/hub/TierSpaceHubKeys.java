package com.obm.network.tierspace.hub;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class TierSpaceHubKeys {

    public static final String META_EQUIPPED = "obm_tierspace_hub_equipped";

    private static NamespacedKey hubItemKey;

    private TierSpaceHubKeys() {
    }

    public static void register(Plugin plugin) {
        hubItemKey = new NamespacedKey(plugin, "hub_item");
    }

    public static NamespacedKey hubItemKey() {
        return hubItemKey;
    }
}

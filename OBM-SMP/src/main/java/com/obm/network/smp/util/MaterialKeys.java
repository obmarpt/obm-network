package com.obm.network.smp.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.Locale;

public final class MaterialKeys {

    private MaterialKeys() {
    }

    /**
     * Resolve nomes de config (OAK_LOG, oak_log, minecraft:oak_log) para Material.
     */
    public static Material resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String trimmed = raw.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);

        Material material = Material.matchMaterial(upper);
        if (isValid(material)) {
            return material;
        }

        material = Material.matchMaterial(trimmed.toLowerCase(Locale.ROOT));
        if (isValid(material)) {
            return material;
        }

        if (trimmed.contains(":")) {
            String keyPart = trimmed.substring(trimmed.indexOf(':') + 1).toLowerCase(Locale.ROOT);
            NamespacedKey key = NamespacedKey.minecraft(keyPart);
            material = Registry.MATERIAL.get(key);
            if (isValid(material)) {
                return material;
            }
        }

        try {
            material = Material.valueOf(upper);
            return isValid(material) ? material : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isValid(Material material) {
        return material != null && !material.isAir();
    }
}

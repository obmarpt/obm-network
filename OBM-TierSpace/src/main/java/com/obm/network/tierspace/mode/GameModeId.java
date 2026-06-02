package com.obm.network.tierspace.mode;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum GameModeId {
    SWORD("sword", "Sword PvP", "⚔", Material.IRON_SWORD, 10),
    NODEBUFF("nodebuff", "NoDebuff PvP", "💧", Material.SPLASH_POTION, 11),
    UHC("uhc", "UHC PvP", "🏹", Material.BOW, 12),
    AXE("axe", "Axe PvP", "🪓", Material.IRON_AXE, 13),
    NETHERITE("netherite", "Netherite PvP", "🔥", Material.NETHERITE_CHESTPLATE, 19),
    CRYSTAL("crystal", "Crystal PvP", "💥", Material.END_CRYSTAL, 20),
    MACE("mace", "Mace PvP", "⚡", resolveMaterial("MACE", Material.NETHERITE_AXE), 21);

    private final String id;
    private final String defaultDisplayName;
    private final String icon;
    private final Material iconMaterial;
    private final int defaultGuiSlot;

    GameModeId(String id, String defaultDisplayName, String icon, Material iconMaterial, int defaultGuiSlot) {
        this.id = id;
        this.defaultDisplayName = defaultDisplayName;
        this.icon = icon;
        this.iconMaterial = iconMaterial;
        this.defaultGuiSlot = defaultGuiSlot;
    }

    public String id() {
        return id;
    }

    public String defaultDisplayName() {
        return defaultDisplayName;
    }

    /** Fallback when ModeRegistry is unavailable. */
    public String displayName() {
        return defaultDisplayName;
    }

    public String icon() {
        return icon;
    }

    public Material iconMaterial() {
        return iconMaterial;
    }

    public int defaultGuiSlot() {
        return defaultGuiSlot;
    }

    public static List<GameModeId> ordered() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(GameModeId::defaultGuiSlot))
                .toList();
    }

    public static Optional<GameModeId> from(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(mode -> mode.id.equals(normalized))
                .findFirst();
    }

    private static Material resolveMaterial(String name, Material fallback) {
        Material material = Material.matchMaterial(name);
        return material == null ? fallback : material;
    }
}

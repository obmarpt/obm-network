package com.obm.network.tierspace.mode;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public enum GameModeId {
    SWORD("sword", "Sword PvP", "⚔", Material.IRON_SWORD, 10),
    AXE("axe", "Axe PvP", "🪓", Material.IRON_AXE, 11),
    MACE("mace", "Mace PvP", "⚡", resolveMaterial("MACE", Material.NETHERITE_AXE), 12),
    UHC("uhc", "UHC PvP", "🏹", Material.BOW, 13),
    NETHERITE("netherite", "Netherite PvP", "🔥", Material.NETHERITE_CHESTPLATE, 14),
    OP("op", "OP PvP", "✦", Material.ENCHANTED_GOLDEN_APPLE, 15),
    POT("pot", "Pot PvP", "🧪", Material.SPLASH_POTION, 16),
    SMP("smp", "SMP PvP", "⛏", Material.IRON_PICKAXE, 19),
    VANILLA("vanilla", "CPvP", "🗡", Material.WOODEN_SWORD, 20),
    /** Legado — desactivado por defeito no config. */
    NODEBUFF("nodebuff", "NoDebuff PvP", "💧", Material.SPLASH_POTION, 21),
    CRYSTAL("crystal", "Crystal PvP", "💥", Material.END_CRYSTAL, 22);

    private static final Map<String, GameModeId> ALIASES = Map.of(
            "cpvp", VANILLA,
            "vanilla", VANILLA,
            "pot", POT,
            "nodebuff", POT,
            "nb", POT
    );

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

    /** Fallback quando {@link com.obm.network.tierspace.mode.ModeRegistry} não está disponível. */
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
                .filter(m -> m != NODEBUFF && m != CRYSTAL)
                .sorted(Comparator.comparingInt(GameModeId::defaultGuiSlot))
                .toList();
    }

    public static Optional<GameModeId> from(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        GameModeId alias = ALIASES.get(normalized);
        if (alias != null) {
            return Optional.of(alias);
        }
        return Arrays.stream(values())
                .filter(mode -> mode.id.equals(normalized))
                .findFirst();
    }

    private static Material resolveMaterial(String name, Material fallback) {
        Material material = Material.matchMaterial(name);
        return material == null ? fallback : material;
    }
}

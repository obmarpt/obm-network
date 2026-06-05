package com.obm.network.tierspace.mode;

import com.obm.network.tierspace.ui.TierMenuColors;
import org.bukkit.ChatColor;

public final class TierModeVisual {

    private TierModeVisual() {
    }

    public static String coloredName(GameModeId mode, String displayName) {
        return bold(color(mode)) + displayName;
    }

    public static String color(GameModeId mode) {
        return switch (mode) {
            case SWORD -> "§9";
            case AXE -> "§e";
            case MACE -> "§6";
            case UHC -> "§c";
            case NETHERITE -> "§8";
            case OP -> "§b";
            case POT, NODEBUFF -> "§5";
            case SMP -> "§a";
            case VANILLA -> "§f";
            case CRYSTAL -> "§d";
        };
    }

    public static String bold(String colorCode) {
        return colorCode + "§l";
    }

    public static String hexAccent(GameModeId mode) {
        return switch (mode) {
            case SWORD -> TierMenuColors.hex("#3399FF", "");
            case AXE -> TierMenuColors.hex("#FFDD55", "");
            case MACE -> TierMenuColors.hex("#FF8800", "");
            case UHC -> TierMenuColors.hex("#FF4444", "");
            case NETHERITE -> TierMenuColors.hex("#888888", "");
            case OP -> TierMenuColors.hex("#55FFFF", "");
            case POT, NODEBUFF -> TierMenuColors.hex("#AA55FF", "");
            case SMP -> TierMenuColors.hex("#55FF55", "");
            case VANILLA -> TierMenuColors.hex("#FFFFFF", "");
            case CRYSTAL -> TierMenuColors.hex("#FF55FF", "");
        };
    }

    public static String queueStatusLine(int queueSize, boolean playerQueued) {
        if (playerQueued) {
            return ChatColor.GREEN + "A procurar...";
        }
        if (queueSize > 0) {
            return ChatColor.GRAY + "Aguarda match...";
        }
        return ChatColor.DARK_GRAY + "A aguardar...";
    }
}

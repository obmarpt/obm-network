package com.obm.network.lobby.gui;

/**
 * Paleta visual MineSpace — HEX convertido para formato legacy §x (Paper 1.16+).
 */
public final class MenuColors {

    // Modos
    public static final String RUSH = "#FFAA00";
    public static final String HARDCORE = "#FF4444";
    public static final String TIERSPACE = "#00AAFF";
    public static final String TIERSPACE_ACCENT = "#AA55FF";

    // Neutros
    public static final String NEUTRAL = "#AAAAAA";
    public static final String DARK = "#555555";
    public static final String WHITE = "#FFFFFF";
    public static final String SUCCESS = "#55FF55";
    public static final String ERROR = "#FF5555";

    private MenuColors() {
    }

    /** Converte #RRGGBB + texto para §x§R§R§G§G§B§Btexto */
    public static String hex(String color, String text) {
        String c = color.startsWith("#") ? color.substring(1) : color;
        if (c.length() != 6) {
            return text;
        }
        StringBuilder out = new StringBuilder("§x");
        for (char ch : c.toCharArray()) {
            out.append('§').append(ch);
        }
        return out.append(text).toString();
    }

    public static String bold(String coloredText) {
        return "§l" + coloredText;
    }

    public static String rush(String text) {
        return hex(RUSH, text);
    }

    public static String hardcore(String text) {
        return hex(HARDCORE, text);
    }

    public static String tier(String text) {
        return hex(TIERSPACE, text);
    }

    public static String tierAccent(String text) {
        return hex(TIERSPACE_ACCENT, text);
    }

    public static String neutral(String text) {
        return hex(NEUTRAL, text);
    }

    public static String white(String text) {
        return hex(WHITE, text);
    }

    public static String success(String text) {
        return hex(SUCCESS, text);
    }

    public static String error(String text) {
        return hex(ERROR, text);
    }

    public static String separator() {
        return hex(DARK, "━━━━━━━━━━━━━━━━");
    }

    public static String action() {
        return rush("▶ Clique para abrir");
    }

    public static String actionTier() {
        return tier("▶ Clique para entrar");
    }
}

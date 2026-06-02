package com.obm.network.tierspace.ui;

public final class TierMenuColors {

    public static final String PRIMARY = "#00AAFF";
    public static final String ACCENT = "#AA55FF";
    public static final String NEUTRAL = "#AAAAAA";
    public static final String DARK = "#555555";
    public static final String WHITE = "#FFFFFF";
    public static final String SUCCESS = "#55FF55";
    public static final String ERROR = "#FF4444";
    public static final String WIN = "#55FF55";
    public static final String LOSS = "#FF4444";

    private TierMenuColors() {
    }

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

    public static String bold(String text) {
        return "§l" + text;
    }

    public static String primary(String text) {
        return hex(PRIMARY, text);
    }

    public static String accent(String text) {
        return hex(ACCENT, text);
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
}

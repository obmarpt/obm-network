package com.obm.network.lobby.gui;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MenuText {

    private MenuText() {
    }

    public static String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> colorizeLore(List<String> lines) {
        List<String> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            out.add(colorize(line));
        }
        return out;
    }

    public static String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) {
            return "";
        }
        String result = text;
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                result = result.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return colorize(result);
    }

    public static List<String> applyPlaceholders(List<String> lines, Map<String, String> placeholders) {
        List<String> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            out.add(applyPlaceholders(line, placeholders));
        }
        return out;
    }
}

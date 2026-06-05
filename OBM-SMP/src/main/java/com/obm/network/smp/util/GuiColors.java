package com.obm.network.smp.util;

public final class GuiColors {

    private GuiColors() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }
        return input.replace('&', '§').replace('?', '§');
    }
}

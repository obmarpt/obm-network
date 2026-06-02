package com.obm.network.lobby.gui;

public final class MenuTitles {

    /** Inventory title — 27 slots */
    public static final String MAIN = MenuColors.bold(
            MenuColors.tier("✦ ") + MenuColors.tierAccent("Mine") + MenuColors.tier("Space") + MenuColors.tier(" ✦"));
    public static final String RUSH = MenuColors.bold(MenuColors.rush("RUSH SMP"));
    public static final String HARDCORE = MenuColors.bold(MenuColors.hardcore("HARDCORE"));
    public static final String PROFILE = MenuColors.bold(MenuColors.hex("#FFD700", "PERFIL"));

    private MenuTitles() {
    }
}

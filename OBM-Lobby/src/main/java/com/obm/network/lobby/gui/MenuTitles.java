package com.obm.network.lobby.gui;

public final class MenuTitles {

    /** Título do menu principal — definido em config.yml (main-menu.title). */
    public static String main() {
        return MainMenuConfig.title();
    }
    public static final String RUSH = MenuColors.bold(MenuColors.rush("RUSH SMP"));
    public static final String HARDCORE = MenuColors.bold(MenuColors.hardcore("HARDCORE"));
    public static final String PROFILE = MenuColors.bold(MenuColors.hex("#FFD700", "PERFIL"));

    private MenuTitles() {
    }
}

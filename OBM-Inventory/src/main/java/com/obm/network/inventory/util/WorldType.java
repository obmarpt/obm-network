package com.obm.network.inventory.util;

public enum WorldType {

    LOBBY("Lobby"),
    UHC("uhc"),
    SMP("smp");

    private final String name;

    WorldType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
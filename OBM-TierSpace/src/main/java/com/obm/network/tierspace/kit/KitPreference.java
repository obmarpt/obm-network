package com.obm.network.tierspace.kit;

public enum KitPreference {
    DEFAULT,
    CUSTOM,
    RANDOM;

    public static KitPreference fromString(String raw) {
        if (raw == null) {
            return DEFAULT;
        }
        try {
            return KitPreference.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }
}

package com.obm.network.smp.duel;

import java.util.UUID;

public final class DuelInvite {

    private final UUID challenger;
    private final UUID challenged;
    private final long expiresAt;

    public DuelInvite(UUID challenger, UUID challenged, long expiresAt) {
        this.challenger = challenger;
        this.challenged = challenged;
        this.expiresAt = expiresAt;
    }

    public UUID challenger() {
        return challenger;
    }

    public UUID challenged() {
        return challenged;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}

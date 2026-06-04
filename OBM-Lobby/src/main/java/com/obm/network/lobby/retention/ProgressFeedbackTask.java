package com.obm.network.lobby.retention;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Sessão de retenção (near-unlock). Action bar contínua foi removida — ver {@code TemporaryActionBar} no OBM-SMP.
 */
public class ProgressFeedbackTask {

    private final Set<UUID> nearUnlockNotified = new HashSet<>();

    public void clearSession(UUID uuid) {
        nearUnlockNotified.remove(uuid);
    }
}

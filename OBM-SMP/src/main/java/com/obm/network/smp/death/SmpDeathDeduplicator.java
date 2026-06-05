package com.obm.network.smp.death;

import com.obm.network.smp.SMPPlugin;
import org.bukkit.Bukkit;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Garante que cada morte SMP (victim UUID) só dispara recompensa/stats uma vez.
 */
public final class SmpDeathDeduplicator {

    private static final long RELEASE_TICKS = 100L;

    private final Set<UUID> processedDeaths = ConcurrentHashMap.newKeySet();

    /**
     * @return true se esta morte ainda não foi processada
     */
    public boolean tryClaim(UUID victimId) {
        return processedDeaths.add(victimId);
    }

    public void releaseLater(UUID victimId) {
        SMPPlugin plugin = SMPPlugin.get();
        if (plugin == null || !plugin.isEnabled()) {
            processedDeaths.remove(victimId);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> processedDeaths.remove(victimId), RELEASE_TICKS);
    }
}

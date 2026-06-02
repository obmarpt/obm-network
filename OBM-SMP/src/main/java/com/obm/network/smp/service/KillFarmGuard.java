package com.obm.network.smp.service;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KillFarmGuard {

    private final long cooldownMs;
    private final Map<String, Long> recentKills = new ConcurrentHashMap<>();

    public KillFarmGuard(FileConfiguration config) {
        long minutes = config.getLong("exploits.kill-cooldown-minutes", 30L);
        this.cooldownMs = minutes * 60L * 1000L;
    }

    public boolean canRewardKill(UUID killer, UUID victim) {
        if (cooldownMs <= 0) {
            return true;
        }
        String key = killer + ":" + victim;
        Long last = recentKills.get(key);
        long now = System.currentTimeMillis();
        if (last != null && now - last < cooldownMs) {
            return false;
        }
        recentKills.put(key, now);
        return true;
    }

    public long remainingCooldownSeconds(UUID killer, UUID victim) {
        String key = killer + ":" + victim;
        Long last = recentKills.get(key);
        if (last == null) {
            return 0;
        }
        long remaining = cooldownMs - (System.currentTimeMillis() - last);
        return Math.max(0, remaining / 1000L);
    }
}

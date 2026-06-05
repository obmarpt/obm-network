package com.obm.network.smp.listener;

import com.obm.network.smp.service.SpawnProtectionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

public final class SpawnBuildProtectionListener implements Listener {

    private final SpawnProtectionService spawnProtection;

    public SpawnBuildProtectionListener(SpawnProtectionService spawnProtection) {
        this.spawnProtection = spawnProtection;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (shouldProtect(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNão podes construir na zona segura.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (shouldProtect(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNão podes quebrar blocos na zona segura.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent event) {
        if (shouldProtect(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNão podes usar baldes na zona segura.");
        }
    }

    private boolean shouldProtect(Player player) {
        if (player == null || !spawnProtection.isEnabled()) {
            return false;
        }
        return spawnProtection.isInSpawnProtection(player.getLocation());
    }
}

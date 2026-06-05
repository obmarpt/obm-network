package com.obm.network.smp.listener;

import com.obm.network.smp.spawn.SpawnBoundaryManager;
import com.obm.network.smp.service.SpawnProtectionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class SpawnBoundaryListener implements Listener {

    private final SpawnProtectionService protection;
    private final SpawnBoundaryManager boundaryManager;

    public SpawnBoundaryListener(SpawnProtectionService protection, SpawnBoundaryManager boundaryManager) {
        this.protection = protection;
        this.boundaryManager = boundaryManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (boundaryManager.blocks().isBoundaryBlock(event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNão podes alterar o limite da zona segura.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (boundaryManager.blocks().isBoundaryBlock(event.getBlock())
                || boundaryManager.blocks().isBoundaryBlock(event.getBlockAgainst())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!protection.settings().blockExit()) {
            return;
        }
        Player player = event.getPlayer();
        if (event.getTo() == null) {
            return;
        }
        if (!protection.isInSpawnProtection(event.getFrom())) {
            return;
        }
        if (!protection.isOutsideBoundary(event.getTo())) {
            return;
        }
        event.setTo(protection.clampInside(event.getFrom()));
    }
}

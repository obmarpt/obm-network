package com.obm.network.smp.listener;

import com.obm.network.core.combat.CombatLogService;
import com.obm.network.core.state.PlayerStateBridge;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.duel.DuelService;
import com.obm.network.smp.manager.SMPManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class SMPListener implements Listener {

    private final SMPManager manager;
    private final WorldModeService worldModeService;
    private final DuelService duelService;

    public SMPListener(SMPManager manager, WorldModeService worldModeService, DuelService duelService) {
        this.manager = manager;
        this.worldModeService = worldModeService;
        this.duelService = duelService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (duelService != null && duelService.isInDuel(victim.getUniqueId())) {
            return;
        }
        if (PlayerStateBridge.isInDuel(victim)) {
            return;
        }
        if (!worldModeService.isSMP(victim.getWorld().getName())) {
            return;
        }
        CombatLogService.clearCombat(victim);
        manager.handleDeath(victim, victim.getKiller());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (worldModeService.isSMP(event.getPlayer().getWorld().getName())) {
            manager.handleJoin(event.getPlayer());
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (worldModeService.isSMP(event.getPlayer().getWorld().getName())) {
            manager.handleJoin(event.getPlayer());
        }
    }
}

package com.obm.network.tierspace.listener;

import com.obm.network.tierspace.match.MatchService;
import com.obm.network.tierspace.queue.QueueService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MatchListener implements Listener {

    private final MatchService matchService;
    private final QueueService queueService;

    public MatchListener(MatchService matchService, QueueService queueService) {
        this.matchService = matchService;
        this.queueService = queueService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!matchService.isInMatch(victim.getUniqueId())) {
            return;
        }

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player killer = victim.getKiller();
        matchService.handleDeath(victim, killer);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        queueService.leave(player.getUniqueId());
        matchService.handleDisconnect(player);
    }
}

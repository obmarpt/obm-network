package com.obm.network.tierspace.anticheat;

import com.obm.network.tierspace.match.MatchService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Set;

public class MatchProtectionListener implements Listener {

    private static final Set<String> BLOCKED_ROOTS = Set.of(
            "spawn", "lobby", "tp", "teleport", "gamemode", "gm"
    );

    private final MatchService matchService;
    private final MatchProtectionService matchProtectionService;

    public MatchProtectionListener(MatchService matchService, MatchProtectionService matchProtectionService) {
        this.matchService = matchService;
        this.matchProtectionService = matchProtectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!matchService.isInMatch(player.getUniqueId())) {
            return;
        }

        String command = event.getMessage().toLowerCase(Locale.ROOT).trim();
        if (!command.startsWith("/")) {
            return;
        }

        String root = command.substring(1).split("\\s+")[0];
        if (root.contains(":")) {
            root = root.substring(root.indexOf(':') + 1);
        }

        if (BLOCKED_ROOTS.contains(root)) {
            event.setCancelled(true);
            player.sendMessage("§cComando bloqueado durante o match.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (matchProtectionService.isCountdownFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}

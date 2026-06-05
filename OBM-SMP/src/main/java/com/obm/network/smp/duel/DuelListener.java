package com.obm.network.smp.duel;

import com.obm.network.core.state.PlayerStateBridge;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

public final class DuelListener implements Listener {

    private static final Set<String> ALLOWED_COMMANDS = Set.of("duel");

    private final DuelService duelService;

    public DuelListener(DuelService duelService) {
        this.duelService = duelService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        duelService.handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!duelService.isInDuel(victim.getUniqueId())) {
            return;
        }

        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        Player killer = victim.getKiller();
        duelService.handleDeath(victim, killer);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        duelService.handleDisconnect(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        if (!duelService.isInDuel(player.getUniqueId())) {
            return;
        }
        ItemStack broken = event.getBrokenItem();
        if (broken != null && !broken.getType().isAir()) {
            duelService.trackBrokenItem(player.getUniqueId(), broken);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!duelService.isInDuel(player.getUniqueId())) {
            return;
        }
        Item dropped = event.getItemDrop();
        ItemStack stack = dropped.getItemStack();
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        duelService.trackSecuredDrop(player.getUniqueId(), stack.clone());
        dropped.remove();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!duelService.isDuelWorld(player.getWorld().getName())) {
            return;
        }
        if (!duelService.isInDuel(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        UUID throwerId = event.getItem().getThrower();
        if (throwerId != null && duelService.isInDuel(throwerId)) {
            UUID opponent = duelService.getDuel(player.getUniqueId())
                    .map(d -> d.opponent(player.getUniqueId()))
                    .orElse(null);
            if (opponent != null && throwerId.equals(opponent)) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!duelService.isDuelWorld(player.getWorld().getName())) {
            return;
        }
        if (duelService.isInDuel(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event);
        if (attacker == null) {
            return;
        }

        boolean victimInDuel = duelService.isInDuel(victim.getUniqueId());
        boolean attackerInDuel = duelService.isInDuel(attacker.getUniqueId());

        if (victimInDuel || attackerInDuel) {
            if (!victimInDuel || !attackerInDuel) {
                event.setCancelled(true);
                return;
            }
            if (!duelService.isRoundActive(victim.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            UUID opponent = duelService.getDuel(victim.getUniqueId())
                    .map(d -> d.opponent(victim.getUniqueId()))
                    .orElse(null);
            if (opponent == null || !opponent.equals(attacker.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!duelService.isInDuel(player.getUniqueId())) {
            return;
        }
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage("§cNão podes teleportar durante um duelo.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!duelService.isInDuel(player.getUniqueId()) && !PlayerStateBridge.isInDuel(player)) {
            return;
        }
        String msg = event.getMessage();
        if (msg == null || msg.length() < 2) {
            return;
        }
        String cmd = msg.substring(1).split(" ")[0].toLowerCase();
        if (ALLOWED_COMMANDS.contains(cmd)) {
            return;
        }
        if (player.hasPermission("obm.smp.admin")) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage("§cComandos bloqueados durante o duelo.");
    }

    private static Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}

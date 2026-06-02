package com.obm.network.smp.listener;

import com.obm.network.smp.service.SpawnProtectionService;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SpawnProtectionListener implements Listener {

    private final SpawnProtectionService spawnProtectionService;

    public SpawnProtectionListener(SpawnProtectionService spawnProtectionService) {
        this.spawnProtectionService = spawnProtectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof Player victim)) return;

        // 🔥 IGNORAR ENDER PEARL
        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getType() == EntityType.ENDER_PEARL) return;
        }

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        // 🔥 IGNORAR SELF-DAMAGE
        if (attacker.equals(victim)) return;

        if (!spawnProtectionService.isPvpAllowed(attacker, victim)) {
            event.setCancelled(true);
            attacker.sendMessage("§cPvP está desativado no spawn.");
        }
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player player) {
            return player;
        }

        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                return shooter;
            }
        }

        return null;
    }
}
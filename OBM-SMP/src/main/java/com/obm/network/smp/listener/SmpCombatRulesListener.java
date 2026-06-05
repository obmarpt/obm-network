package com.obm.network.smp.listener;

import com.obm.network.core.state.TierSpaceStateHelper;
import com.obm.network.core.world.WorldModeService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Rush SMP / Hardcore: sem crystal, anchor ou explosivos PvP.
 * TierSpace ranked fica isento (crystal só lá).
 */
public final class SmpCombatRulesListener implements Listener {

    private static final String MSG = "§cCrystal, anchor e explosivos estão desativados neste modo.";

    private final WorldModeService worldModeService;

    public SmpCombatRulesListener(WorldModeService worldModeService) {
        this.worldModeService = worldModeService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!applies(event.getPlayer())) {
            return;
        }
        Material type = event.getBlock().getType();
        if (isRestrictedMaterial(type)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MSG);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!applies(player)) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        Material type = item.getType();
        if (!isRestrictedMaterial(type)) {
            return;
        }
        if (type == Material.END_CRYSTAL && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            if (clicked != null && (clicked.getType() == Material.OBSIDIAN
                    || clicked.getType() == Material.BEDROCK)) {
                event.setCancelled(true);
                player.sendMessage(MSG);
            }
            return;
        }
        if (type == Material.RESPAWN_ANCHOR
                || type == Material.TNT
                || type == Material.TNT_MINECART) {
            event.setCancelled(true);
            player.sendMessage(MSG);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) {
            return;
        }
        if (!(tnt.getSource() instanceof Player player)) {
            return;
        }
        if (applies(player)) {
            event.setCancelled(true);
            player.sendMessage(MSG);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getLocation().getWorld() == null) {
            return;
        }
        String world = event.getLocation().getWorld().getName();
        if (!worldModeService.isSMP(world) && !worldModeService.isUHC(world)) {
            return;
        }
        EntityType cause = event.getEntity() != null ? event.getEntity().getType() : null;
        if (cause == EntityType.END_CRYSTAL
                || cause == EntityType.TNT
                || cause == EntityType.TNT_MINECART) {
            event.setCancelled(true);
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof EnderCrystal)) {
            return;
        }
        if (event.getEntity() instanceof Player victim && applies(victim)) {
            event.setCancelled(true);
        }
    }

    private boolean applies(Player player) {
        if (player == null || player.getWorld() == null) {
            return false;
        }
        String world = player.getWorld().getName();
        if (worldModeService.isRanked(world)) {
            return false;
        }
        if (TierSpaceStateHelper.isInTierSpaceMatch(player)) {
            return false;
        }
        if (!worldModeService.isSMP(world) && !worldModeService.isUHC(world)) {
            return false;
        }
        return true;
    }

    private static boolean isRestrictedMaterial(Material type) {
        return type == Material.END_CRYSTAL
                || type == Material.RESPAWN_ANCHOR
                || type == Material.TNT
                || type == Material.TNT_MINECART;
    }
}

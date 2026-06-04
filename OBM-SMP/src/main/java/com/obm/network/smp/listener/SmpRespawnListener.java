package com.obm.network.smp.listener;

import com.obm.network.core.combat.CombatLogService;
import com.obm.network.core.location.SafeSpawnService;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.SMPPlugin;
import com.obm.network.smp.shop.ShopGui;
import com.obm.network.smp.shop.session.ShopSessionManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Estabiliza o jogador após respawn no SMP para reduzir falsos positivos (ex.: Spartan gravity-simulation).
 */
public final class SmpRespawnListener implements Listener {

    private final SMPPlugin plugin;
    private final WorldModeService worldModeService;
    private final ShopSessionManager shopSessions;

    public SmpRespawnListener(SMPPlugin plugin, WorldModeService worldModeService, ShopGui shopGui) {
        this.plugin = plugin;
        this.worldModeService = worldModeService;
        this.shopSessions = shopGui.getSessionManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!worldModeService.isSMP(player.getWorld().getName())) {
            return;
        }
        CombatLogService.clearCombat(player);
        shopSessions.remove(player.getUniqueId());
        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        World world = event.getRespawnLocation() != null
                ? event.getRespawnLocation().getWorld()
                : player.getWorld();
        if (world == null || !worldModeService.isSMP(world.getName())) {
            return;
        }

        if (plugin.getConfig().getBoolean("respawn.use-safe-spawn", true)) {
            Location base = event.getRespawnLocation() != null
                    ? event.getRespawnLocation()
                    : world.getSpawnLocation();
            Location safe = SafeSpawnService.getSafeSpawn(world, base);
            if (safe != null) {
                event.setRespawnLocation(safe);
            }
        }
    }

    /* Reset completo pós-respawn: PlayerStateResetListener (OBM-Core) */
}

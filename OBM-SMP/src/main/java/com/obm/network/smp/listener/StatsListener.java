package com.obm.network.smp.listener;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import com.obm.network.core.storage.PlayerStatsKeys;
import com.obm.network.core.world.WorldModeService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StatsListener implements Listener {

    private final DataStore dataStore;
    private final WorldModeService worldModeService;

    public StatsListener() {
        this.dataStore = OBMCorePlugin.get().getDataStore();
        this.worldModeService = OBMCorePlugin.get().getWorldModeService();
    }

    /*
     * ✅ BLOCK BREAK
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {

        Player player = e.getPlayer();

        if (!isRush(player)) return;

        dataStore.increment(player.getUniqueId(), PlayerStatsKeys.BLOCKS_BROKEN_SMP);
    }

    /*
     * ✅ DAMAGE (DEALT + TAKEN)
     */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Player damager)) return;
        if (!(e.getEntity() instanceof Player victim)) return;

        if (!isRush(damager)) return;

        double damage = e.getFinalDamage();

        UUID damagerId = damager.getUniqueId();
        UUID victimId = victim.getUniqueId();

        // dealt
        int dealt = dataStore.getInt(damagerId, "damage_dealt_rush");
        dataStore.set(damagerId, "damage_dealt_rush", dealt + (int) damage);

        // taken
        int taken = dataStore.getInt(victimId, "damage_taken_rush");
        dataStore.set(victimId, "damage_taken_rush", taken + (int) damage);
    }

    /*
     * ✅ TOTEM USAGE
     */
    @EventHandler
    public void onTotem(EntityResurrectEvent e) {

        if (!(e.getEntity() instanceof Player player)) return;
        if (!e.isCancelled()) {

            if (!isRush(player)) return;

            dataStore.increment(player.getUniqueId(), "totems_used_rush");
        }
    }

    /*
     * ✅ SHIELD DISABLE
     */
    @EventHandler
    public void onShieldBreak(EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof Player victim)) return;

        if (!isRush(attacker)) return;

        // shield active
        if (victim.isBlocking()) {

            // strong hit (critério simplificado)
            if (e.getDamage() >= 3.0) {
                dataStore.increment(attacker.getUniqueId(), "shields_disabled_rush");
            }
        }
    }

    /*
     * ✅ MODE CHECK
     */
    private boolean isRush(Player player) {
        return worldModeService.isSMP(player.getWorld().getName());
    }
}
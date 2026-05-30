package com.obm.network.smp.listener;

import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.service.EconomyService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class RushListener implements Listener {

    private static final int DEATH_PENALTY = 3000;
    private static final int KILL_REWARD = 10000;

    private final EconomyService economyService;
    private final WorldModeService worldModeService;

    public RushListener(EconomyService economyService, WorldModeService worldModeService) {
        this.economyService = economyService;
        this.worldModeService = worldModeService;
    }

    @EventHandler
    public void onRushPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        if (!worldModeService.isRush(deceased.getWorld().getName())) {
            return;
        }

        economyService.withdraw(deceased.getUniqueId(), DEATH_PENALTY);
        deceased.sendMessage("§cVocê perdeu §e" + DEATH_PENALTY + " coins §cpor morrer no Rush SMP.");

        Player killer = deceased.getKiller();
        if (killer != null && !killer.getUniqueId().equals(deceased.getUniqueId())) {
            economyService.deposit(killer.getUniqueId(), KILL_REWARD);
            killer.sendMessage("§aVocê recebeu §e" + KILL_REWARD + " coins §apor matar alguém no Rush SMP.");
        }
    }
}

package com.obm.network.lobby.retention;

import com.obm.network.core.hardcore.HardcoreUnlockService;
import com.obm.network.core.hardcore.HardcoreUnlockStatus;
import com.obm.network.lobby.OBMLobbyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Objetivos visíveis ao entrar + limpar sessão near-unlock.
 */
public class RetentionJoinListener implements Listener {

    private final ProgressFeedbackTask feedbackTask;

    public RetentionJoinListener(ProgressFeedbackTask feedbackTask) {
        this.feedbackTask = feedbackTask;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        HardcoreUnlockStatus hc = HardcoreUnlockService.evaluate(player.getUniqueId());

        if (!hc.gateEnabled() || hc.unlocked()) {
            return;
        }

        OBMLobbyPlugin plugin = OBMLobbyPlugin.get();
        if (plugin != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> sendObjectives(player, hc), 40L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        feedbackTask.clearSession(event.getPlayer().getUniqueId());
    }

    private void sendObjectives(org.bukkit.entity.Player player, HardcoreUnlockStatus hc) {
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§c§l🎯 Objetivo: Desbloquear Hardcore");
        player.sendMessage("");
        player.sendMessage("§7Hardcore desbloqueia em:");
        player.sendMessage("§fLevel: §e" + hc.playerLevel() + "§7/§f" + hc.requiredLevel());
        player.sendMessage("§fMoney: §6" + HardcoreUnlockService.formatMoney(hc.playerCoins())
                + "§7/§e" + HardcoreUnlockService.formatMoney(hc.requiredCoins()));
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}

package com.obm.network.lobby.retention;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.hardcore.HardcoreUnlockService;
import com.obm.network.core.hardcore.HardcoreUnlockStatus;
import com.obm.network.core.integration.EconomyBridge;
import com.obm.network.core.integration.SMPBridge;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.lobby.OBMLobbyPlugin;
import com.obm.network.smp.retention.RetentionFeedback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Action bar com coins, level e progresso Hardcore (apenas leitura de sistemas existentes).
 */
public class ProgressFeedbackTask extends BukkitRunnable {

    private static final double NEAR_UNLOCK_RATIO = 0.90;

    private final Set<UUID> nearUnlockNotified = new HashSet<>();

    public void start(OBMLobbyPlugin plugin) {
        runTaskTimer(plugin, 40L, 60L);
    }

    @Override
    public void run() {
        WorldModeService worlds = OBMCorePlugin.get().getWorldModeService();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String world = player.getWorld().getName();
            if (!worlds.isLobby(world) && !worlds.isSMP(world)) {
                continue;
            }

            UUID uuid = player.getUniqueId();
            int coins = EconomyBridge.getBalance(uuid);
            SMPBridge.ProgressionSnapshot prog = SMPBridge.getProgression(uuid);
            int level = prog.level();

            HardcoreUnlockStatus hc = HardcoreUnlockService.evaluate(uuid);
            String bar = buildActionBar(coins, level, prog.xp(), prog.xpRequired(), hc);
            RetentionFeedback.sendActionBar(player, bar);

            checkNearUnlock(player, uuid, hc);
        }
    }

    private String buildActionBar(int coins, int level, int xp, int xpRequired,
                                  HardcoreUnlockStatus hc) {
        StringBuilder sb = new StringBuilder();
        sb.append("§6").append(RetentionFeedback.formatCoins(coins)).append(" coins");
        sb.append(" §8| §eLv ").append(level);
        if (xpRequired > 0 && xpRequired < Integer.MAX_VALUE / 2) {
            sb.append(" §7(").append(xp).append("/").append(xpRequired).append(" XP)");
        }

        if (hc.gateEnabled() && !hc.unlocked()) {
            sb.append(" §8| §cHC §e").append(hc.playerLevel()).append("§7/§f").append(hc.requiredLevel());
            sb.append(" §6").append(HardcoreUnlockService.formatCoins(hc.playerCoins()));
            sb.append("§7/§e").append(HardcoreUnlockService.formatCoins(hc.requiredCoins()));
        } else if (hc.gateEnabled()) {
            sb.append(" §8| §aHC ✓");
        }

        return sb.toString();
    }

    private void checkNearUnlock(Player player, UUID uuid, HardcoreUnlockStatus hc) {
        if (!hc.gateEnabled() || hc.unlocked() || nearUnlockNotified.contains(uuid)) {
            return;
        }

        double levelRatio = hc.requiredLevel() <= 0 ? 1.0 : (double) hc.playerLevel() / hc.requiredLevel();
        double coinRatio = hc.requiredCoins() <= 0 ? 1.0 : (double) hc.playerCoins() / hc.requiredCoins();
        double progress = Math.min(levelRatio, coinRatio);

        if (progress < NEAR_UNLOCK_RATIO) {
            return;
        }

        nearUnlockNotified.add(uuid);
        player.sendMessage("§eEstás quase a desbloquear o Hardcore!");
    }

    public void clearSession(UUID uuid) {
        nearUnlockNotified.remove(uuid);
    }
}

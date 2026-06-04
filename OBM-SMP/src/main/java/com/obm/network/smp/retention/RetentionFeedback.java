package com.obm.network.smp.retention;

import com.obm.network.smp.SMPPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Feedback visual/audio leve (coins, daily, milestones).
 */
public final class RetentionFeedback {

    private RetentionFeedback() {
    }

    public static void coinsGained(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        String formatted = formatCoins(amount);
        player.sendMessage("§a+§e" + formatted + " coins§a!");
        showTemporary(player, "§a+§e" + formatted + " coins", TemporaryActionBar.DEFAULT_TICKS);
    }

    public static void dailySuccess(Player player, int amount) {
        player.sendMessage("");
        player.sendMessage("§a§l✅ Recebeste " + formatCoins(amount) + " coins");
        player.sendMessage("§7Volta amanhã para mais recompensas!");
        player.sendMessage("");
        player.sendTitle("§a§l✅ DAILY", "§e+" + formatCoins(amount) + " coins", 5, 40, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        showTemporary(player, "§a✅ Daily reward collected!", 100);
    }

    public static void dailyAlreadyClaimed(Player player, String timeLeft) {
        player.sendMessage("§c§l❌ Já recolheste hoje");
        player.sendMessage("§7Próxima recompensa em §f" + timeLeft);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.6f);
    }

    public static void playtimeMilestone(Player player, int minutes, int coins) {
        player.sendMessage("§a§l🎁 " + minutes + " min jogados!");
        player.sendMessage("§7Recebeste §e" + formatCoins(coins) + " coins§7.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.0f);
        showTemporary(player, "§6+" + formatCoins(coins) + " coins §7| §e" + minutes + "min playtime", 100);
    }

    public static void levelUp(Player player, int level, int coinsReward) {
        player.sendMessage("§a§l⬆ LEVEL UP! §eNível " + level);
        if (coinsReward > 0) {
            player.sendMessage("§7Bónus: §e+" + formatCoins(coinsReward) + " coins");
        }
        player.sendTitle("§6§lLEVEL UP", "§eNível " + level, 10, 50, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        showTemporary(player, "§6§lLEVEL UP §8| §eNível " + level, 100);
    }

    public static void xpGained(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        showTemporary(player, "§a+§f" + amount + " XP", 60);
    }

    public static void smpWelcome(Player player, int coins, int level) {
        showTemporary(player,
                "§6" + formatCoins(coins) + " coins §8| §eLv " + level + " §8| §7Bem-vindo ao Rush",
                100);
    }

    public static void sendActionBar(Player player, String message) {
        try {
            player.sendActionBar(message);
        } catch (NoSuchMethodError ignored) {
            player.sendMessage(message);
        }
    }

    private static void showTemporary(Player player, String message, int ticks) {
        SMPPlugin plugin = SMPPlugin.get();
        if (plugin != null && plugin.isEnabled()) {
            TemporaryActionBar.show(plugin, player, message, ticks);
        } else {
            sendActionBar(player, message);
        }
    }

    public static String formatCoins(int amount) {
        if (amount >= 1_000_000) {
            return String.format(Locale.US, "%,d", amount);
        }
        if (amount >= 1_000) {
            return String.format(Locale.US, "%,d", amount);
        }
        return String.valueOf(amount);
    }
}

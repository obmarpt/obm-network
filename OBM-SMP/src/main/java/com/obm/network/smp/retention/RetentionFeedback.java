package com.obm.network.smp.retention;

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
        sendActionBar(player, "§a+§e" + formatted + " coins");
    }

    public static void dailySuccess(Player player, int amount) {
        player.sendMessage("");
        player.sendMessage("§a§l✅ Recebeste " + formatCoins(amount) + " coins");
        player.sendMessage("§7Volta amanhã para mais recompensas!");
        player.sendMessage("");
        player.sendTitle("§a§l✅ DAILY", "§e+" + formatCoins(amount) + " coins", 5, 40, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        sendActionBar(player, "§a✅ Daily reward collected!");
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
        sendActionBar(player, "§6+" + formatCoins(coins) + " coins §7| §e" + minutes + "min playtime");
    }

    public static void levelUp(Player player, int level, int coinsReward) {
        player.sendMessage("§a§l⬆ LEVEL UP! §eNível " + level);
        if (coinsReward > 0) {
            player.sendMessage("§7Bónus: §e+" + formatCoins(coinsReward) + " coins");
        }
        player.sendTitle("§6§lLEVEL UP", "§eNível " + level, 10, 50, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        sendActionBar(player, "§6§lLEVEL UP §8| §eNível " + level);
    }

    public static void sendActionBar(Player player, String message) {
        try {
            player.sendActionBar(message);
        } catch (NoSuchMethodError ignored) {
            player.sendMessage(message);
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

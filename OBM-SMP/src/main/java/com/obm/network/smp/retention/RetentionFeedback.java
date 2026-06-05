package com.obm.network.smp.retention;

import com.obm.network.core.economy.CurrencyLabels;
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
        coinsGained(player, amount, null);
    }

    public static void coinsGained(Player player, int amount, String reason) {
        if (amount <= 0) {
            return;
        }
        String coins = CurrencyLabels.formatAmount(amount);
        String actionBar = reason == null || reason.isBlank()
                ? "§a+" + coins + " coins!"
                : "§a+" + coins + " coins §7(" + reason + ")";
        showTemporary(player, actionBar, TemporaryActionBar.DEFAULT_TICKS);
    }

    public static void coinsLost(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        String coins = CurrencyLabels.formatAmount(amount);
        showTemporary(player, "§c-" + coins + " coins", TemporaryActionBar.DEFAULT_TICKS);
    }

    public static void dailySuccess(Player player, int amount) {
        player.sendMessage("");
        player.sendMessage("§a§l✅ Recebeste " + CurrencyLabels.formatSmpMoney(amount));
        player.sendMessage("§7Volta amanhã para mais recompensas!");
        player.sendMessage("");
        player.sendTitle("§a§l✅ DAILY", "§e+" + CurrencyLabels.formatSmpMoney(amount), 5, 40, 10);
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
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.0f);
        coinsGained(player, coins, minutes + "min");
    }

    public static void levelUp(Player player, int level, int coinsReward) {
        player.sendMessage("§a§l⬆ SMP LEVEL UP! §eSMP Level " + level);
        if (coinsReward > 0) {
            player.sendMessage("§7Bónus: §e+" + CurrencyLabels.formatSmpMoney(coinsReward));
        }
        player.sendTitle("§6§lSMP LEVEL UP", "§aSMP Level §f" + level, 10, 50, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        showTemporary(player, "§6§lSMP LEVEL §8| §aSMP Level §f" + level, 100);
    }

    public static void xpGained(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        showTemporary(player, "§a+§f" + amount + " XP", 60);
    }

    public static void smpWelcome(Player player, int coins, int level) {
        showTemporary(player,
                "§6" + CurrencyLabels.formatSmpMoney(coins) + " §8| §eLv " + level + " §8| §7Bem-vindo ao Rush",
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

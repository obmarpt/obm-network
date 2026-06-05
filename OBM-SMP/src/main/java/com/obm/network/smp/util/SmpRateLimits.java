package com.obm.network.smp.util;

import com.obm.network.smp.SMPPlugin;
import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cooldowns anti-abuso para pay e market (config: economy.pay.*, market.listing-cooldown-seconds).
 */
public final class SmpRateLimits {

    private static final Map<UUID, Long> LAST_PAY_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_MARKET_LIST_MS = new ConcurrentHashMap<>();

    private SmpRateLimits() {
    }

    public static PayCheckResult checkPay(Player player, int amount) {
        SMPPlugin plugin = SMPPlugin.get();
        if (plugin == null) {
            return PayCheckResult.ok();
        }
        int cooldownSec = effectivePayCooldown(player);
        if (cooldownSec > 0) {
            long last = LAST_PAY_MS.getOrDefault(player.getUniqueId(), 0L);
            long elapsed = System.currentTimeMillis() - last;
            if (elapsed < cooldownSec * 1000L) {
                int wait = (int) Math.ceil((cooldownSec * 1000L - elapsed) / 1000.0);
                return PayCheckResult.denied("§cAguarda §f" + wait + "s §cantes de voltar a pagar.");
            }
        }

        int dailyLimit = plugin.getConfig().getInt("economy.pay.daily-send-limit", 50_000);
        if (dailyLimit > 0) {
            int sentToday = getPaySentToday(player.getUniqueId());
            if (sentToday + amount > dailyLimit) {
                return PayCheckResult.denied("§cLimite diário de envio: §f" + dailyLimit
                        + " Money§c (já enviaste §f" + sentToday + "§c).");
            }
        }
        return PayCheckResult.ok();
    }

    public static void recordPay(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        LAST_PAY_MS.put(uuid, System.currentTimeMillis());
        if (amount <= 0) {
            return;
        }
        SMPPlugin plugin = SMPPlugin.get();
        if (plugin == null || plugin.getConfig().getInt("economy.pay.daily-send-limit", 50_000) <= 0) {
            return;
        }
        DataStore ds = OBMCorePlugin.get().getDataStore();
        String dayKey = dayKey();
        String amountKey = "smp_pay_sent_" + dayKey;
        int current = ds.getInt(uuid, amountKey);
        ds.set(uuid, amountKey, current + amount);
        ds.save(uuid);

        int logThreshold = plugin.getConfig().getInt("economy.pay.log-amount-threshold", 10_000);
        if (logThreshold > 0 && amount >= logThreshold) {
            plugin.getLogger().info("[OBM-SMP] PAY " + player.getName() + " sent " + amount + " coins");
        }
    }

    public static MarketCheckResult checkMarketListing(Player player) {
        SMPPlugin plugin = SMPPlugin.get();
        if (plugin == null) {
            return MarketCheckResult.ok();
        }
        int cooldownSec = effectiveMarketCooldown(player);
        if (cooldownSec <= 0) {
            return MarketCheckResult.ok();
        }
        long last = LAST_MARKET_LIST_MS.getOrDefault(player.getUniqueId(), 0L);
        long elapsed = System.currentTimeMillis() - last;
        if (elapsed < cooldownSec * 1000L) {
            int wait = (int) Math.ceil((cooldownSec * 1000L - elapsed) / 1000.0);
            return MarketCheckResult.denied("§cAguarda §f" + wait + "s §cantes de criar outra listagem.");
        }
        return MarketCheckResult.ok();
    }

    public static void recordMarketListing(Player player) {
        LAST_MARKET_LIST_MS.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private static int getPaySentToday(UUID uuid) {
        DataStore ds = OBMCorePlugin.get().getDataStore();
        return ds.getInt(uuid, "smp_pay_sent_" + dayKey());
    }

    private static String dayKey() {
        return LocalDate.now().toString();
    }

    private static int effectivePayCooldown(Player player) {
        SMPPlugin plugin = SMPPlugin.get();
        if (plugin == null) {
            return 0;
        }
        int base = plugin.getConfig().getInt("economy.pay.cooldown-seconds", 30);
        return applyRankCooldownReduction(player, base);
    }

    private static int effectiveMarketCooldown(Player player) {
        SMPPlugin plugin = SMPPlugin.get();
        if (plugin == null) {
            return 0;
        }
        int base = plugin.getConfig().getInt("market.listing-cooldown-seconds", 45);
        return applyRankCooldownReduction(player, base);
    }

    private static int applyRankCooldownReduction(Player player, int baseSeconds) {
        if (baseSeconds <= 0 || player == null) {
            return baseSeconds;
        }
        try {
            var rankService = SMPPlugin.get().getRankService();
            double reduction = rankService.getCooldownReduction(player.getUniqueId());
            return Math.max(5, (int) Math.round(baseSeconds * (1.0 - reduction)));
        } catch (Exception ignored) {
            return baseSeconds;
        }
    }

    public record PayCheckResult(boolean allowed, String message) {
        public static PayCheckResult ok() {
            return new PayCheckResult(true, null);
        }

        public static PayCheckResult denied(String message) {
            return new PayCheckResult(false, message);
        }
    }

    public record MarketCheckResult(boolean allowed, String message) {
        public static MarketCheckResult ok() {
            return new MarketCheckResult(true, null);
        }

        public static MarketCheckResult denied(String message) {
            return new MarketCheckResult(false, message);
        }
    }
}

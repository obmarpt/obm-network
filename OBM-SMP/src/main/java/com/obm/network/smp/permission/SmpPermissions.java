package com.obm.network.smp.permission;

import com.obm.network.core.ui.PlayerUx;
import com.obm.network.smp.SMPPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SmpPermissions {

    public static final String ENTER = "obm.smp.enter";
    public static final String SHOP = "obm.smp.shop";
    public static final String SELL = "obm.smp.sell";
    public static final String AUCTION = "obm.smp.auction";
    public static final String RANK = "obm.smp.rank";
    public static final String MONEY = "obm.smp.money";
    public static final String MONEY_PAY = "obm.smp.money.pay";
    public static final String MARKET = "obm.smp.market";
    public static final String TOP = "obm.smp.top";
    public static final String STATS = "obm.stats";
    public static final String VIP = "obm.smp.vip";
    public static final String DUEL = "obm.duel.use";
    public static final String RANKUP = "obm.rankup.use";
    public static final String ADMIN = "obm.smp.admin";

    private SmpPermissions() {
    }

    public static boolean isAdmin(CommandSender sender) {
        return sender.hasPermission(ADMIN);
    }

    public static boolean has(CommandSender sender, String permission) {
        if (isAdmin(sender)) {
            return true;
        }
        return sender.hasPermission(permission);
    }

    public static boolean hasVip(Player player) {
        return has(player, VIP);
    }

    public static boolean deny(CommandSender sender, String permission) {
        return deny(sender, permission, null);
    }

    public static boolean deny(CommandSender sender, String permission, String context) {
        if (has(sender, permission)) {
            return false;
        }
        PlayerUx.error(sender, "Não tens permissão para usar isto.");
        if (context != null && !context.isBlank()) {
            PlayerUx.hint(sender, context);
        }
        if (sender instanceof Player p) {
            PlayerUx.errorSound(p);
        }
        return true;
    }

    public static boolean deny(Player player, String permission, String context) {
        if (has(player, permission)) {
            return false;
        }
        PlayerUx.error(player, "Não tens permissão para usar isto.");
        if (context != null && !context.isBlank()) {
            PlayerUx.hint(player, context);
        }
        PlayerUx.errorSound(player);
        return true;
    }

    public static double vipCoinMultiplier(Player player) {
        if (player == null) {
            return 1.0D;
        }
        double tier = com.obm.network.core.integration.VipTierBridge.moneyMultiplier(player.getUniqueId());
        if (tier > 1.0D) {
            return tier;
        }
        if (!hasVip(player)) {
            return 1.0D;
        }
        SMPPlugin plugin = SMPPlugin.get();
        if (plugin == null) {
            return 1.1D;
        }
        return plugin.getConfig().getDouble("vip.coin-multiplier", 1.1D);
    }

    public static void debug(Player player, String message) {
        if (player == null || !isAdmin(player)) {
            return;
        }
        SMPPlugin plugin = SMPPlugin.get();
        if (plugin != null && plugin.getConfig().getBoolean("admin.debug", true)) {
            player.sendMessage("§8[OBM-SMP] §7" + message);
        }
    }
}

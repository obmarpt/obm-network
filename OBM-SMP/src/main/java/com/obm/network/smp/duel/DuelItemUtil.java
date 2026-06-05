package com.obm.network.smp.duel;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;

final class DuelItemUtil {

    private static final double BROKEN_REMAINING_FRACTION = 0.05;

    private DuelItemUtil() {
    }

    static ItemStack asLowDurabilityReturn(ItemStack broken) {
        if (broken == null || broken.getType().isAir()) {
            return null;
        }
        ItemStack restored = broken.clone();
        int max = restored.getType().getMaxDurability();
        if (max <= 0) {
            return restored;
        }
        ItemMeta meta = restored.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int damage = (int) Math.floor(max * (1.0 - BROKEN_REMAINING_FRACTION));
            damageable.setDamage(Math.min(Math.max(damage, 0), max - 1));
            restored.setItemMeta(damageable);
        }
        return restored;
    }

    static void giveSafely(Player player, ItemStack item) {
        if (player == null || !player.isOnline() || item == null || item.getType().isAir()) {
            return;
        }
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
    }
}

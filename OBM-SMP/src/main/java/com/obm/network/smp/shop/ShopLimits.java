package com.obm.network.smp.shop;

import com.obm.network.smp.SMPPlugin;

public final class ShopLimits {

    private ShopLimits() {
    }

    public static int maxQuantity() {
        SMPPlugin plugin = SMPPlugin.get();
        if (plugin == null) {
            return 64;
        }
        return Math.max(1, plugin.getConfig().getInt("shop.max-quantity", 64));
    }
}

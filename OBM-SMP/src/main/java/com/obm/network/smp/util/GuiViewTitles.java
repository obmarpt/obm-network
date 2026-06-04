package com.obm.network.smp.util;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.InventoryView;

/**
 * Resolves inventory titles consistently across Paper versions (legacy String vs Adventure).
 */
public final class GuiViewTitles {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private GuiViewTitles() {
    }

    public static String resolve(InventoryView view) {
        if (view == null) {
            return "";
        }
        try {
            String title = view.getTitle();
            if (title != null && !title.isEmpty()) {
                return title;
            }
        } catch (Throwable ignored) {
        }
        try {
            return LEGACY.serialize(view.title());
        } catch (Throwable ignored) {
        }
        return "";
    }

    public static boolean isSmpShop(InventoryView view) {
        String title = resolve(view);
        return GuiTitles.SHOP_CATEGORIES.equals(title) || GuiTitles.SHOP_ITEMS.equals(title);
    }
}

package com.obm.network.smp.shop;

import com.obm.network.smp.service.ShopCatalog;
import com.obm.network.smp.service.ShopService;
import com.obm.network.smp.shop.session.ShopSessionManager;
import org.bukkit.entity.Player;

public final class ShopGui {

    private final CategoryGui categoryGui;
    private final ShopSessionManager sessionManager;

    public ShopGui(ShopCatalog catalog, ShopService shopService) {
        this.sessionManager = new ShopSessionManager();
        this.categoryGui = new CategoryGui(catalog, shopService, sessionManager);
    }

    public void openCategories(Player player) {
        categoryGui.openMain(player);
    }

    public void openItems(Player player, String mainCategory) {
        categoryGui.openItems(player, mainCategory);
    }

    public void refreshItems(Player player) {
        categoryGui.refreshItems(player);
    }

    public CategoryGui getCategoryGui() {
        return categoryGui;
    }

    public ShopSessionManager getSessionManager() {
        return sessionManager;
    }
}

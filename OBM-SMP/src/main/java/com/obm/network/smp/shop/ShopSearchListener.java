package com.obm.network.smp.shop;

import com.obm.network.smp.permission.SmpPermissions;
import com.obm.network.smp.shop.session.ShopSession;
import com.obm.network.smp.shop.session.ShopSessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ShopSearchListener implements Listener {

    private final ShopGui shopGui;
    private final ShopSessionManager sessionManager;

    public ShopSearchListener(ShopGui shopGui) {
        this.shopGui = shopGui;
        this.sessionManager = shopGui.getSessionManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ShopSession session = sessionManager.get(player.getUniqueId());
        if (session == null || !session.isAwaitingSearch()) {
            return;
        }
        if (!SmpPermissions.has(player, SmpPermissions.SHOP)) {
            session.setAwaitingSearch(false);
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancelar") || message.equalsIgnoreCase("cancel")) {
            session.setSearchQuery("");
        } else {
            session.setSearchQuery(message);
        }
        session.setAwaitingSearch(false);

        org.bukkit.Bukkit.getScheduler().runTask(
                com.obm.network.smp.SMPPlugin.get(),
                () -> shopGui.openItems(player, session.getCurrentCategory()));
    }
}

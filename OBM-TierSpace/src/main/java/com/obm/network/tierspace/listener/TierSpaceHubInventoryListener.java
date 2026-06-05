package com.obm.network.tierspace.listener;

import com.obm.network.tierspace.hub.TierSpaceHub;
import com.obm.network.tierspace.ui.RankedHubItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public final class TierSpaceHubInventoryListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!TierSpaceHub.isInTierSpaceHub(player)) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() < topSize) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!TierSpaceHub.isInTierSpaceHub(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!TierSpaceHub.isInTierSpaceHub(event.getPlayer())) {
            return;
        }
        if (RankedHubItems.isHubItem(event.getMainHandItem())
                || RankedHubItems.isHubItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }
}

package com.obm.network.smp.gui;

import com.obm.network.smp.SMPPlugin;
import com.obm.network.smp.gui.RushShopMenu.ShopEntry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RushShopListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (title == null || !title.equals("§6§lLoja Rush")) return;
        e.setCancelled(true);

        ItemStack item = e.getCurrentItem();
        if (item == null) return;

        // handle back arrow
        if (item.getType() == org.bukkit.Material.ARROW) {
            if (e.getWhoClicked() instanceof org.bukkit.entity.Player p) p.closeInventory();
            return;
        }

        // find matching shop entry by display name
        for (ShopEntry entry : RushShopMenu.getShop().values()) {
            if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                    && item.getItemMeta().getDisplayName().equals(entry.display.getItemMeta().getDisplayName())) {

                var econ = SMPPlugin.get().getEconomyService();
                var uuid = e.getWhoClicked().getUniqueId();
                if (!econ.canAfford(uuid, entry.cost)) {
                    e.getWhoClicked().sendMessage("§cSaldo insuficiente. Precisas de §e" + entry.cost + " coins§c.");
                    return;
                }

                ItemStack[] itemsToGive = entry.items.stream().map(ItemStack::clone).toArray(ItemStack[]::new);
                if (!e.getWhoClicked().getInventory().addItem(itemsToGive).isEmpty()) {
                    e.getWhoClicked().sendMessage("§cInventário cheio.");
                    return;
                }

                econ.withdraw(uuid, entry.cost);
                e.getWhoClicked().sendMessage("§aCompra realizada: §e" + entry.displayName + " §7por §f" + entry.cost + " coins.");
                return;
            }
        }
    }
}

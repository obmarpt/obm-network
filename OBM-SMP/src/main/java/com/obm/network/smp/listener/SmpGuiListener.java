package com.obm.network.smp.listener;

import com.obm.network.smp.auction.AuctionGui;
import com.obm.network.smp.auction.AuctionGui.AuctionSession;
import com.obm.network.smp.sell.SellGui;
import com.obm.network.smp.service.AuctionService;
import com.obm.network.smp.service.MarketListing;
import com.obm.network.smp.service.ShopCatalog;
import com.obm.network.smp.service.ShopService;
import com.obm.network.smp.service.SellService;
import com.obm.network.smp.shop.ShopGui;
import com.obm.network.smp.util.GuiTitles;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SmpGuiListener implements Listener {

    private final ShopGui shopGui;
    private final ShopService shopService;
    private final ShopCatalog shopCatalog;
    private final SellGui sellGui;
    private final SellService sellService;
    private final AuctionGui auctionGui;
    private final AuctionService auctionService;

    public SmpGuiListener(ShopGui shopGui, ShopService shopService, ShopCatalog shopCatalog,
                          SellGui sellGui, SellService sellService,
                          AuctionGui auctionGui, AuctionService auctionService) {
        this.shopGui = shopGui;
        this.shopService = shopService;
        this.shopCatalog = shopCatalog;
        this.sellGui = sellGui;
        this.sellService = sellService;
        this.auctionGui = auctionGui;
        this.auctionService = auctionService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (title == null) {
            return;
        }

        if (title.equals(GuiTitles.SHOP_CATEGORIES)) {
            handleShopCategories(event, player);
        } else if (GuiTitles.isShopCategory(title)) {
            handleShopCategory(event, player, GuiTitles.categoryFromTitle(title));
        } else if (title.equals(GuiTitles.SHOP_QUANTITY)) {
            handleShopQuantity(event, player);
        } else if (title.equals(GuiTitles.SHOP_CONFIRM)) {
            handleShopConfirm(event, player);
        } else if (title.equals(GuiTitles.SELL)) {
            handleSell(event, player);
        } else if (title.equals(GuiTitles.AUCTION_MAIN)) {
            handleAuctionMain(event, player);
        } else if (title.equals(GuiTitles.AUCTION_BROWSE)) {
            handleAuctionBrowse(event, player);
        } else if (title.equals(GuiTitles.AUCTION_SELL)) {
            handleAuctionSell(event, player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (GuiTitles.SELL.equals(title)) {
            returnItemsFromSell(player, event.getInventory());
        }
        if (GuiTitles.AUCTION_SELL.equals(title)) {
            ItemStack item = event.getInventory().getItem(13);
            if (item != null && !item.getType().isAir()) {
                player.getInventory().addItem(item);
            }
            auctionGui.clearSellSession(player.getUniqueId());
        }
        if (title != null && (title.startsWith("§6Loja") || title.startsWith("§dLeilão"))) {
            shopService.clearSession(player.getUniqueId());
        }
    }

    private void handleShopCategories(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        for (String category : shopCatalog.getCategories()) {
            if (clicked.getItemMeta().getDisplayName().equalsIgnoreCase("§e" + capitalize(category))) {
                shopGui.openCategory(player, category);
                return;
            }
        }
    }

    private void handleShopCategory(InventoryClickEvent event, Player player, String category) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        if (clicked.getType() == Material.ARROW) {
            shopGui.openCategories(player);
            return;
        }
        Material material = clicked.getType();
        if (material.isAir() || !shopCatalog.getItems(category).containsKey(material)) {
            return;
        }
        if (!shopCatalog.canPurchase(material)) {
            player.sendMessage("§cEste item não está disponível para compra.");
            return;
        }
        shopGui.openQuantity(player, category, material);
    }

    private void handleShopQuantity(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        var session = shopService.getSession(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }
        if (clicked.getType() == Material.ARROW) {
            shopGui.openCategory(player, session.category());
            return;
        }
        if (clicked.getType() == Material.LIME_CONCRETE) {
            shopGui.openConfirm(player, session.category(), session.material(), session.quantity());
            return;
        }
        if (clicked.getType() == Material.PAPER && clicked.hasItemMeta()) {
            String name = clicked.getItemMeta().getDisplayName();
            int quantity = parseQuantity(name);
            if (quantity > 0) {
                shopGui.openConfirm(player, session.category(), session.material(), quantity);
            }
        }
    }

    private void handleShopConfirm(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        var session = shopService.getSession(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }
        if (clicked.getType() == Material.RED_CONCRETE) {
            shopGui.openQuantity(player, session.category(), session.material());
            return;
        }
        if (clicked.getType() == Material.LIME_CONCRETE) {
            var result = shopService.purchase(player, session.category(), session.material(), session.quantity());
            player.sendMessage(result.message());
            player.closeInventory();
        }
    }

    private void handleSell(InventoryClickEvent event, Player player) {
        int rawSlot = event.getRawSlot();
        Inventory top = event.getView().getTopInventory();

        if (rawSlot >= top.getSize()) {
            if (rawSlot >= SellGui.INPUT_START && rawSlot <= SellGui.INPUT_END) {
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                        com.obm.network.smp.SMPPlugin.get(),
                        () -> sellGui.refreshFooter(player, top, collectSellItems(top)),
                        1L);
            }
            return;
        }

        if (rawSlot == SellGui.CONFIRM_SLOT) {
            event.setCancelled(true);
            ItemStack[] items = collectSellItems(top);
            var result = sellService.sellItems(player, items);
            player.sendMessage(result.message());
            if (result.success()) {
            for (int i = SellGui.INPUT_START; i <= SellGui.INPUT_END; i++) {
                top.setItem(i, null);
            }
        }
            sellGui.refreshFooter(player, top, collectSellItems(top));
            return;
        }

        if (rawSlot == SellGui.CANCEL_SLOT) {
            event.setCancelled(true);
            returnItemsFromSell(player, top);
            player.closeInventory();
            return;
        }

        if (rawSlot == SellGui.INFO_SLOT) {
            event.setCancelled(true);
            return;
        }

        if (rawSlot >= SellGui.INPUT_START && rawSlot <= SellGui.INPUT_END) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                    com.obm.network.smp.SMPPlugin.get(),
                    () -> sellGui.refreshFooter(player, top, collectSellItems(top)),
                    1L);
            return;
        }

        event.setCancelled(true);
    }

    private void handleAuctionMain(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        switch (clicked.getType()) {
            case CHEST -> auctionGui.openBrowse(player);
            case EMERALD -> auctionGui.openSell(player);
            case BARRIER -> player.closeInventory();
            default -> {}
        }
    }

    private void handleAuctionBrowse(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        if (clicked.getType() == Material.ARROW) {
            auctionGui.openMain(player);
            return;
        }
        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasLore()) {
            return;
        }
        String listingId = extractListingId(clicked);
        if (listingId == null) {
            return;
        }
        var result = auctionService.buyListing(player, listingId);
        player.sendMessage(result.getMessage());
        if (result.isSuccess()) {
            auctionGui.openBrowse(player);
        }
    }

    private void handleAuctionSell(InventoryClickEvent event, Player player) {
        int rawSlot = event.getRawSlot();
        Inventory top = event.getView().getTopInventory();

        if (rawSlot == 13 && event.getClickedInventory() == top) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                    com.obm.network.smp.SMPPlugin.get(),
                    () -> {
                        ItemStack item = top.getItem(13);
                        if (item != null && !item.getType().isAir()) {
                            AuctionSession current = auctionGui.getSellSession(player.getUniqueId());
                            int price = current != null ? current.price() : auctionService.getMinPrice();
                            auctionGui.setSellSession(player.getUniqueId(), new AuctionSession(item.clone(), price));
                        }
                    },
                    1L);
            return;
        }

        if (rawSlot >= top.getSize()) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            ItemStack item = top.getItem(13);
            if (item != null && !item.getType().isAir()) {
                player.getInventory().addItem(item);
                top.setItem(13, null);
            }
            auctionGui.clearSellSession(player.getUniqueId());
            auctionGui.openMain(player);
            return;
        }

        if (clicked.getType() == Material.GOLD_NUGGET) {
            int price = parseQuantity(clicked.getItemMeta().getDisplayName().replace("§e", "").replace(" coins", ""));
            ItemStack item = top.getItem(13);
            if (item == null || item.getType().isAir()) {
                player.sendMessage("§cColoca um item na slot central primeiro.");
                return;
            }
            auctionGui.setSellSession(player.getUniqueId(), new AuctionSession(item.clone(), price));
            player.sendMessage("§aPreço definido: §e" + price + " coins§a.");
            return;
        }

        if (clicked.getType() == Material.LIME_CONCRETE) {
            ItemStack item = top.getItem(13);
            AuctionSession session = auctionGui.getSellSession(player.getUniqueId());
            int price = session != null ? session.price() : 0;
            if (item == null || item.getType().isAir()) {
                player.sendMessage("§cColoca um item para anunciar.");
                return;
            }
            if (price < auctionService.getMinPrice() || price > auctionService.getMaxPrice()) {
                player.sendMessage("§cDefine um preço válido entre " + auctionService.getMinPrice()
                        + " e " + auctionService.getMaxPrice() + ".");
                return;
            }
            var result = auctionService.createListing(player, item, price);
            player.sendMessage(result.getMessage());
            if (result.isSuccess()) {
                top.setItem(13, null);
                auctionGui.clearSellSession(player.getUniqueId());
                player.closeInventory();
            }
        }
    }

    private ItemStack[] collectSellItems(Inventory inventory) {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = SellGui.INPUT_START; i <= SellGui.INPUT_END; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                items.add(item.clone());
            }
        }
        return items.toArray(new ItemStack[0]);
    }

    private void clearSellInput(Inventory inventory) {
        for (int i = SellGui.INPUT_START; i <= SellGui.INPUT_END; i++) {
            inventory.setItem(i, null);
        }
    }

    private void returnItemsFromSell(Player player, Inventory inventory) {
        for (int i = SellGui.INPUT_START; i <= SellGui.INPUT_END; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                player.getInventory().addItem(item);
                inventory.setItem(i, null);
            }
        }
    }

    private int parseQuantity(String displayName) {
        try {
            return Integer.parseInt(displayName.replace("§e", "").replace("x", "").trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private String extractListingId(ItemStack item) {
        for (String line : item.getItemMeta().getLore()) {
            if (line.startsWith("§7ID: §f")) {
                return line.substring("§7ID: §f".length());
            }
        }
        return null;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}

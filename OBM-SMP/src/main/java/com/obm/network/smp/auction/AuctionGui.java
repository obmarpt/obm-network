package com.obm.network.smp.auction;

import com.obm.network.smp.service.AuctionService;
import com.obm.network.smp.service.MarketListing;
import com.obm.network.smp.util.GuiItems;
import com.obm.network.smp.util.GuiTitles;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionGui {

    public record AuctionSession(ItemStack item, int price) {
    }

    private final AuctionService auctionService;
    private final java.util.Map<UUID, AuctionSession> sellSessions = new ConcurrentHashMap<>();

    public AuctionGui(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, GuiTitles.AUCTION_MAIN);
        inventory.setItem(11, GuiItems.named(Material.CHEST, "§aVer listagens", "§7Comprar itens de outros jogadores"));
        inventory.setItem(13, GuiItems.named(Material.EMERALD, "§eAnunciar item", "§7Vender um item no leilão"));
        inventory.setItem(15, GuiItems.named(Material.BARRIER, "§cFechar"));
        player.openInventory(inventory);
    }

    public void openBrowse(Player player) {
        List<MarketListing> listings = new ArrayList<>(auctionService.getListings());
        int size = Math.min(54, Math.max(27, ((listings.size() / 9) + 1) * 9));
        Inventory inventory = Bukkit.createInventory(null, size, GuiTitles.AUCTION_BROWSE);

        for (int i = 0; i < listings.size() && i < size - 9; i++) {
            inventory.setItem(i, listingIcon(listings.get(i)));
        }
        inventory.setItem(size - 5, GuiItems.named(Material.ARROW, "§eVoltar"));
        player.openInventory(inventory);
    }

    public void openSell(Player player) {
        sellSessions.remove(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(null, 27, GuiTitles.AUCTION_SELL);
        inventory.setItem(13, GuiItems.named(Material.HOPPER, "§eColoca o item aqui", "§7Arrasta o item para esta slot"));
        inventory.setItem(10, priceButton(100));
        inventory.setItem(12, priceButton(500));
        inventory.setItem(14, priceButton(1000));
        inventory.setItem(16, priceButton(5000));
        inventory.setItem(22, GuiItems.named(Material.LIME_CONCRETE, "§aConfirmar anúncio"));
        inventory.setItem(26, GuiItems.named(Material.ARROW, "§eVoltar"));
        player.openInventory(inventory);
    }

    public AuctionSession getSellSession(UUID uuid) {
        return sellSessions.get(uuid);
    }

    public void setSellSession(UUID uuid, AuctionSession session) {
        if (session == null) {
            sellSessions.remove(uuid);
        } else {
            sellSessions.put(uuid, session);
        }
    }

    public void clearSellSession(UUID uuid) {
        sellSessions.remove(uuid);
    }

    private ItemStack priceButton(int price) {
        return GuiItems.named(Material.GOLD_NUGGET, "§e" + price + " coins", "§7Clique para definir preço");
    }

    private ItemStack listingIcon(MarketListing listing) {
        ItemStack item = listing.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.getSeller());
            lore.add("§7ID: §f" + listing.getId());
            lore.add("§7Preço: §e" + listing.getPrice() + " coins/un");
            lore.add("§7Total: §a" + listing.getTotalValue() + " coins");
            lore.add("§7Vendedor: §f" + (seller.getName() != null ? seller.getName() : "Desconhecido"));
            lore.add("§aClique para comprar");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

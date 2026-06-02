package com.obm.network.smp.service;

import com.obm.network.smp.service.MarketService.MarketSaleResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.UUID;

public class AuctionService {

    private final MarketService marketService;
    private final EconomyService economyService;
    private final int maxListingsPerPlayer;
    private final int minPrice;
    private final int maxPrice;

    public AuctionService(JavaPlugin plugin, MarketService marketService, EconomyService economyService) {
        this.marketService = marketService;
        this.economyService = economyService;
        this.maxListingsPerPlayer = plugin.getConfig().getInt("auction.max-listings-per-player",
                plugin.getConfig().getInt("market.max-listings-per-player", 10));
        this.minPrice = plugin.getConfig().getInt("auction.min-price",
                plugin.getConfig().getInt("market.min-price", 1));
        this.maxPrice = plugin.getConfig().getInt("auction.max-price",
                plugin.getConfig().getInt("market.max-price", 1000000));
    }

    public Collection<MarketListing> getListings() {
        return marketService.getListings();
    }

    public MarketSaleResult createListing(Player seller, ItemStack item, int price) {
        if (item == null || item.getType().isAir()) {
            return MarketSaleResult.error("Coloca um item válido para anunciar.");
        }

        ItemStack listingItem = item.clone();
        item.setAmount(0);

        MarketSaleResult result = marketService.createListing(
                seller, listingItem, price, maxListingsPerPlayer, minPrice, maxPrice);

        if (!result.isSuccess()) {
            item.setAmount(listingItem.getAmount());
        }
        return result;
    }

    public MarketSaleResult buyListing(Player buyer, String listingId) {
        return marketService.buyListing(buyer, listingId);
    }

    public MarketSaleResult removeListing(Player seller, String listingId) {
        var optional = marketService.getListing(listingId);
        if (optional.isEmpty()) {
            return MarketSaleResult.error("Anúncio não encontrado.");
        }

        MarketListing listing = optional.get();
        if (!listing.getSeller().equals(seller.getUniqueId())) {
            return MarketSaleResult.error("Apenas o dono do anúncio pode removê-lo.");
        }

        MarketSaleResult result = marketService.removeListing(listingId, seller.getUniqueId());
        if (result.isSuccess()) {
            seller.getInventory().addItem(listing.getItem());
        }
        return result;
    }

    public int getMinPrice() {
        return minPrice;
    }

    public int getMaxPrice() {
        return maxPrice;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }
}

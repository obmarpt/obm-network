package com.obm.network.smp.service;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class MarketService {

    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final File file;
    private final YamlConfiguration config;

    public MarketService(JavaPlugin plugin, EconomyService economyService) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.file = new File(plugin.getDataFolder(), "market.yml");

        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao criar market.yml");
                throw new RuntimeException(e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar market.yml");
            e.printStackTrace();
        }
    }

    public Collection<MarketListing> getListings() {
        if (!config.contains("listings")) {
            return List.of();
        }

        return config.getConfigurationSection("listings").getKeys(false).stream()
                .map(this::loadListing)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public Optional<MarketListing> getListing(String id) {
        if (!config.contains("listings." + id)) {
            return Optional.empty();
        }
        return loadListing(id);
    }

    public MarketSaleResult createListing(Player player, ItemStack item, int price, int maxListingsPerPlayer, int minPrice, int maxPrice) {
        if (item == null || item.getType().isAir()) {
            return MarketSaleResult.error("Segure um item para vender.");
        }

        if (item.getAmount() <= 0) {
            return MarketSaleResult.error("O item precisa ter uma quantidade válida.");
        }

        if (price < minPrice || price > maxPrice) {
            return MarketSaleResult.error("O preço deve ser entre " + minPrice + " e " + maxPrice + ".");
        }

        List<MarketListing> playerListings = getListings().stream()
                .filter(listing -> listing.getSeller().equals(player.getUniqueId()))
                .toList();

        if (playerListings.size() >= maxListingsPerPlayer) {
            return MarketSaleResult.error("Você já atingiu o limite de " + maxListingsPerPlayer + " anúncios.");
        }

        String listingId = UUID.randomUUID().toString().split("-")[0];
        String base = "listings." + listingId + ".";

        ItemStack storedItem = item.clone();
        storedItem.setAmount(item.getAmount());

        config.set(base + "seller", player.getUniqueId().toString());
        config.set(base + "item", storedItem);
        config.set(base + "price", price);
        config.set(base + "created_at", System.currentTimeMillis());
        save();

        return MarketSaleResult.success(listingId);
    }

    public MarketSaleResult buyListing(Player buyer, String id) {
        Optional<MarketListing> optional = getListing(id);
        if (optional.isEmpty()) {
            return MarketSaleResult.error("Anúncio não encontrado.");
        }

        MarketListing listing = optional.get();
        if (listing.getSeller().equals(buyer.getUniqueId())) {
            return MarketSaleResult.error("Você não pode comprar seu próprio anúncio.");
        }

        int total = listing.getTotalValue();
        if (!economyService.canAfford(buyer.getUniqueId(), total)) {
            return MarketSaleResult.error("Saldo insuficiente. Você precisa de " + total + " coins.");
        }

        ItemStack itemToGive = listing.getItem().clone();
        var leftovers = buyer.getInventory().addItem(itemToGive);
        if (!leftovers.isEmpty()) {
            return MarketSaleResult.error("Inventário cheio. Libere espaço antes de comprar.");
        }

        economyService.transfer(buyer.getUniqueId(), listing.getSeller(), total);
        removeListing(id, listing.getSeller());
        return MarketSaleResult.success("Comprado com sucesso! Você pagou " + total + " coins.");
    }

    public MarketSaleResult removeListing(String id, UUID owner) {
        Optional<MarketListing> optional = getListing(id);
        if (optional.isEmpty()) {
            return MarketSaleResult.error("Anúncio não encontrado.");
        }

        MarketListing listing = optional.get();
        if (!listing.getSeller().equals(owner)) {
            return MarketSaleResult.error("Apenas o dono do anúncio pode removê-lo.");
        }

        config.set("listings." + id, null);
        save();
        return MarketSaleResult.success("Anúncio removido.");
    }

    private Optional<MarketListing> loadListing(String id) {
        String base = "listings." + id + ".";
        String sellerId = config.getString(base + "seller");
        ItemStack item = config.getItemStack(base + "item");
        int price = config.getInt(base + "price", 0);
        long createdAt = config.getLong(base + "created_at", System.currentTimeMillis());

        if (sellerId == null || item == null || price <= 0) {
            return Optional.empty();
        }

        UUID seller = UUID.fromString(sellerId);
        return Optional.of(new MarketListing(id, seller, item, price, createdAt));
    }

    public static class MarketSaleResult {

        private final boolean success;
        private final String message;

        private MarketSaleResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static MarketSaleResult success(String message) {
            return new MarketSaleResult(true, message);
        }

        public static MarketSaleResult error(String message) {
            return new MarketSaleResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}

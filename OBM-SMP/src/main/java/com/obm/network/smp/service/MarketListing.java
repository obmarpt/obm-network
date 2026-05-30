package com.obm.network.smp.service;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MarketListing {

    private final String id;
    private final UUID seller;
    private final ItemStack item;
    private final int price;
    private final long createdAt;

    public MarketListing(String id, UUID seller, ItemStack item, int price, long createdAt) {
        this.id = id;
        this.seller = seller;
        this.item = item;
        this.price = price;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public UUID getSeller() {
        return seller;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getPrice() {
        return price;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getTotalValue() {
        return price * item.getAmount();
    }
}

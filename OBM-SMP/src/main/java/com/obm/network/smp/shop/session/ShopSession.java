package com.obm.network.smp.shop.session;

import com.obm.network.smp.shop.ShopLimits;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class ShopSession {

    private final UUID playerId;
    private String currentCategory;
    private String searchQuery = "";
    private int page;
    private Material selectedMaterial;
    private int selectedAmount = 1;
    private boolean awaitingSearch;

    public ShopSession(Player player) {
        this.playerId = player.getUniqueId();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getCurrentCategory() {
        return currentCategory;
    }

    public void setCurrentCategory(String currentCategory) {
        this.currentCategory = currentCategory;
        this.page = 0;
        this.searchQuery = "";
        this.selectedMaterial = null;
        this.selectedAmount = 1;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery == null ? "" : searchQuery.trim();
        this.page = 0;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public Material getSelectedMaterial() {
        return selectedMaterial;
    }

    public void selectMaterial(Material material) {
        this.selectedMaterial = material;
        this.selectedAmount = 1;
    }

    public int getSelectedAmount() {
        return selectedAmount;
    }

    public int getMaxQuantity() {
        return ShopLimits.maxQuantity();
    }

    public void setSelectedAmount(int selectedAmount) {
        this.selectedAmount = Math.max(1, Math.min(getMaxQuantity(), selectedAmount));
    }

    public void adjustAmount(int delta) {
        setSelectedAmount(getSelectedAmount() + delta);
    }

    public boolean isAwaitingSearch() {
        return awaitingSearch;
    }

    public void setAwaitingSearch(boolean awaitingSearch) {
        this.awaitingSearch = awaitingSearch;
    }

    public boolean hasSelection() {
        return selectedMaterial != null && !selectedMaterial.isAir();
    }
}

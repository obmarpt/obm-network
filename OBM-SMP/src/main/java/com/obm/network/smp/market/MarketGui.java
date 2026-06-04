package com.obm.network.smp.market;

import com.obm.network.smp.service.MarketListing;
import com.obm.network.smp.service.MarketService;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MarketGui {

    public static final int SIZE = 54;
    public static final int LISTING_START = 0;
    public static final int LISTING_END = 44;
    public static final int LISTINGS_PER_PAGE = LISTING_END - LISTING_START + 1;

    public static final int SLOT_BACK = 45;
    public static final int SLOT_PREV = 48;
    public static final int SLOT_NEXT = 50;
    public static final int SLOT_CLOSE = 53;

    private static final String ID_PREFIX = "§8#";

    private final MarketService marketService;
    private final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

    public MarketGui(MarketService marketService) {
        this.marketService = marketService;
    }

    public void open(Player player) {
        open(player, pages.getOrDefault(player.getUniqueId(), 0));
    }

    public void open(Player player, int page) {
        List<MarketListing> listings = new ArrayList<>(marketService.getListings());
        int maxPage = Math.max(0, (listings.size() - 1) / LISTINGS_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, maxPage));
        pages.put(player.getUniqueId(), safePage);

        String title = GuiTitles.MARKET_PREFIX + " §7(" + (safePage + 1) + "/" + (maxPage + 1) + ")";
        Inventory inv = Bukkit.createInventory(null, SIZE, title);
        fillFiller(inv);

        int start = safePage * LISTINGS_PER_PAGE;
        for (int slot = LISTING_START; slot <= LISTING_END; slot++) {
            int index = start + (slot - LISTING_START);
            if (index >= listings.size()) {
                break;
            }
            inv.setItem(slot, listingDisplay(listings.get(index)));
        }

        inv.setItem(SLOT_BACK, GuiItems.named(Material.ARROW, "§7§l← Voltar", "§7Fecha o mercado"));
        if (safePage > 0) {
            inv.setItem(SLOT_PREV, GuiItems.named(Material.SPECTRAL_ARROW, "§e§l← Página anterior",
                    "§7Página " + safePage));
        }
        if (safePage < maxPage) {
            inv.setItem(SLOT_NEXT, GuiItems.named(Material.SPECTRAL_ARROW, "§e§lPróxima página →",
                    "§7Página " + (safePage + 2)));
        }
        inv.setItem(SLOT_CLOSE, GuiItems.named(Material.BARRIER, "§c§lFechar"));

        if (listings.isEmpty()) {
            inv.setItem(22, GuiItems.named(Material.BARRIER, "§e§lMercado vazio",
                    "§7Usa §f/market sell <preço>",
                    "§7com um item na mão"));
        }

        player.openInventory(inv);
    }

    public boolean isMarketTitle(String title) {
        return title != null && title.startsWith(GuiTitles.MARKET_PREFIX);
    }

    public String extractListingId(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return null;
        }
        for (String line : item.getItemMeta().getLore()) {
            if (line != null && line.startsWith(ID_PREFIX)) {
                return line.substring(ID_PREFIX.length());
            }
        }
        return null;
    }

    public void clearPage(Player player) {
        pages.remove(player.getUniqueId());
    }

    private ItemStack listingDisplay(MarketListing listing) {
        ItemStack item = listing.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String itemName = formatItemName(item.getType());
        meta.setDisplayName("§e§l" + itemName);

        int amount = item.getAmount();
        int unitPrice = listing.getPrice();
        int total = listing.getTotalValue();
        OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.getSeller());
        String sellerName = seller.getName() != null ? seller.getName() : "Desconhecido";

        List<String> lore = new ArrayList<>();
        lore.add("§8━━━━━━━━━━━━━━━━");
        lore.add("§7Quantidade: §f" + amount);
        lore.add("§7Preço: §a" + unitPrice + " §7coins/un");
        lore.add("§7Total: §e" + total + " §7coins");
        lore.add("");
        lore.add("§7Vendedor: §b" + sellerName);
        lore.add("");
        lore.add("§a§l👉 Click para comprar");
        lore.add(ID_PREFIX + listing.getId());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatItemName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return out.toString().trim();
    }

    private void fillFiller(Inventory inv) {
        ItemStack pane = GuiItems.named(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = LISTING_END + 1; i < SIZE; i++) {
            if (i == SLOT_BACK || i == SLOT_PREV || i == SLOT_NEXT || i == SLOT_CLOSE) {
                continue;
            }
            inv.setItem(i, pane);
        }
    }
}

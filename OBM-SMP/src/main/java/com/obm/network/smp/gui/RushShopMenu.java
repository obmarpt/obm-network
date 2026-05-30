package com.obm.network.smp.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RushShopMenu {

    private static final Map<String, ShopEntry> shop = createShop();

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6§lLoja Rush");

        int slot = 11;
        for (ShopEntry e : shop.values()) {
            inv.setItem(slot, e.display.clone());
            slot++;
        }

        inv.setItem(22, createItem(Material.ARROW, "§bVoltar", "§7Retornar ao menu"));

        return inv;
    }

    public static Map<String, ShopEntry> getShop() {
        return shop;
    }

        private static Map<String, ShopEntry> createShop() {
        Map<String, ShopEntry> m = new HashMap<>();
        // PvP category
        m.put("full_iron", new ShopEntry("full_iron", "Full Iron", 5000, List.of(
            createItem(Material.IRON_HELMET, "§7Capacete de Ferro"),
            createItem(Material.IRON_CHESTPLATE, "§7Peitoral de Ferro"),
            createItem(Material.IRON_LEGGINGS, "§7Calças de Ferro"),
            createItem(Material.IRON_BOOTS, "§7Botas de Ferro"),
            createItem(Material.IRON_SWORD, "§7Espada de Ferro")
        )));

        m.put("full_diamond", new ShopEntry("full_diamond", "Full Diamond", 20000, List.of(
            createItem(Material.DIAMOND_HELMET, "§bCapacete Diamond"),
            createItem(Material.DIAMOND_CHESTPLATE, "§bPeitoral Diamond"),
            createItem(Material.DIAMOND_LEGGINGS, "§bCalças Diamond"),
            createItem(Material.DIAMOND_BOOTS, "§bBotas Diamond"),
            createItem(Material.DIAMOND_SWORD, "§bEspada Diamond")
        )));

        // Food
        m.put("gapple", new ShopEntry("gapple", "Gapple x5", 1500, List.of(createItem(Material.GOLDEN_APPLE, "§6Gapple x5", 5))));

        // Utility
        m.put("pearl", new ShopEntry("pearl", "Ender Pearl x16", 1000, List.of(createItem(Material.ENDER_PEARL, "§5Ender Pearl x16", 16))));

        return m;
        }

    private static ItemStack createItem(Material type, String name) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createItem(Material type, String name, int amount) {
        ItemStack item = createItem(type, name);
        item.setAmount(amount);
        return item;
    }

    private static ItemStack createItem(Material type, String name, String lore) {
        ItemStack item = createItem(type, name);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(java.util.List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static class ShopEntry {
        public final String key;
        public final String displayName;
        public final int cost;
        public final List<ItemStack> items;
        public final ItemStack display;

        public ShopEntry(String key, String displayName, int cost, List<ItemStack> items) {
            this.key = key;
            this.displayName = displayName;
            this.cost = cost;
            this.items = items;
            this.display = items.get(0).clone();
        }
    }
}

package com.obm.network.lobby.gui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MenuUtil {

    private MenuUtil() {
    }

    public static void fillAll(Inventory inv, Material pane) {
        ItemStack filler = pane(pane);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    public static void fillBorder(Inventory inv, Material pane) {
        int size = inv.getSize();
        int rows = size / 9;
        ItemStack filler = pane(pane);

        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                inv.setItem(i, filler);
            }
        }
    }

    public static void fillEmpty(Inventory inv, Material pane) {
        ItemStack filler = pane(pane);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }

    public static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§8");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(new ArrayList<>(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }

    public static String formatKd(int kills, int deaths) {
        if (deaths == 0) {
            return String.format("%.2f", (double) kills);
        }
        return String.format("%.2f", (double) kills / deaths);
    }
}

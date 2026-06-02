package com.obm.network.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MainMenu {

    public static Inventory create() {
        Inventory inv = Bukkit.createInventory(null, 27, "§6§lOBM NETWORK");

        inv.setItem(11, createItem(Material.GRASS_BLOCK, "§a§lSMP"));
        inv.setItem(15, createItem(Material.DIAMOND_SWORD, "§c§lUHC"));
        inv.setItem(22, createItem(Material.PLAYER_HEAD, "§e§lPROFILE"));

        return inv;
    }

    private static ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}

package com.obm.network.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

public class UHCMenu {

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§c§lUHC");

        inv.setItem(11, createItem(Material.IRON_SWORD,
                "§cEntrar no UHC",
                "§7Clique para jogar"));

        inv.setItem(13, createItem(Material.GOLDEN_APPLE,
                "§6Estatísticas",
                "§7Wins: §e0",
                "§7Kills: §e0"));

        inv.setItem(15, createItem(Material.BOOK,
                "§eTop UHC",
                "§7Melhores jogadores"));

        inv.setItem(22, createItem(Material.ARROW,
                "§bVoltar",
                "§7Retornar ao menu principal"));

        return inv;
    }

    private static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(java.util.Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
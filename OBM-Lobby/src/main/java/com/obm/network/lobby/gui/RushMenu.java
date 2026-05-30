package com.obm.network.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RushMenu {

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§c§lRUSH SMP");

        inv.setItem(11, createItem(Material.IRON_SWORD,
                "§cEntrar no Rush SMP",
                "§7Clique para jogar no mundo Rush"));

        inv.setItem(13, createItem(Material.GOLDEN_APPLE,
                "§6Loja de Rush",
                "§7Abra a loja de itens para Rush SMP"));

        inv.setItem(15, createItem(Material.BOOK,
                "§eEstatísticas Rush",
                "§7Veja suas estatísticas de Rush"));

        inv.setItem(22, createItem(Material.ARROW,
                "§bVoltar",
                "§7Retornar ao menu principal"));

        return inv;
    }

    private static ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(java.util.List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}

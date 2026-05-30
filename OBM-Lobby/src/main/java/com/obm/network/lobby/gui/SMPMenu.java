package com.obm.network.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;

public class SMPMenu {

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§a§lSMP");

        UUID uuid = player.getUniqueId();
        DataStore ds = OBMCorePlugin.get().getDataStore();

        int time = ds.getInt(uuid, "playtime_smp");
        int blocks = ds.getInt(uuid, "blocks_smp");
        int kills = ds.getInt(uuid, "kills_smp");
        int money = ds.getInt(uuid, "money_smp");

        inv.setItem(10, createItem(Material.CLOCK,
                "§aPlaytime",
                "§7Tempo: §e" + formatTime(time)));

        inv.setItem(12, createItem(Material.GRASS_BLOCK,
                "§aBlocks",
                "§7Blocos: §e" + blocks));

        inv.setItem(14, createItem(Material.EMERALD,
                "§6Dinheiro",
                "§7Coins: §e" + money));

        inv.setItem(16, createItem(Material.IRON_SWORD,
                "§cKills",
                "§7Kills: §e" + kills));

        inv.setItem(13, createItem(Material.COMPASS,
                "§aEntrar no SMP",
                "§7Clique para ir ao mundo SMP"));

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

    private static String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }
}
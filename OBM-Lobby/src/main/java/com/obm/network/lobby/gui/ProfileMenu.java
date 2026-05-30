package com.obm.network.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.entity.Player;

import java.util.UUID;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;

public class ProfileMenu {

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§e§lPROFILE");

        UUID uuid = player.getUniqueId();
        DataStore ds = OBMCorePlugin.get().getDataStore();

        int playtime = ds.getInt(uuid, "playtime");
        int kills = ds.getInt(uuid, "kills");
        int deaths = ds.getInt(uuid, "deaths");
        int blocks = ds.getInt(uuid, "blocks");
        int mobs = ds.getInt(uuid, "mobs");
        int lives = ds.getInt(uuid, "lives");

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName("§e" + player.getName());
            meta.setLore(java.util.List.of(
                    "§7Playtime: §a" + formatTime(playtime),
                    "§7Kills: §c" + kills,
                    "§7Deaths: §f" + deaths,
                    "§7Blocks: §e" + blocks,
                    "§7Mobs: §2" + mobs,
                    "§7Lives: §c❤" + lives,
                    "§7Rank: §bDefault"
            ));
            head.setItemMeta(meta);
        }

        inv.setItem(13, head);
        inv.setItem(22, createItem(Material.ARROW, "§bVoltar", "§7Retornar ao menu principal"));

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

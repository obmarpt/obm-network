package com.obm.network.smp.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class GuiItems {

    private GuiItems() {
    }

    public static ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(List.of(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack shopItem(Material material, int unitPrice, boolean blocked) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f" + formatMaterial(material));
            List<String> lore = new ArrayList<>();
            lore.add("§7Preço: §e" + unitPrice + " coins/un");
            if (blocked) {
                lore.add("§cIndisponível para compra");
            } else {
                lore.add("§aClique para selecionar quantidade");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String formatMaterial(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return builder.toString().trim();
    }
}

package com.obm.network.tierspace.ui;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class RankedHubItems {

    public static final String MARKER_QUEUE = "§8obm:ts_queue";
    public static final String MARKER_PARTY = "§8obm:ts_party";

    private RankedHubItems() {
    }

    public static ItemStack queueSword(String displayName, List<String> lore) {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack partyItem(String displayName, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isQueueSword(ItemStack stack) {
        return hasMarker(stack, MARKER_QUEUE);
    }

    public static boolean isPartyItem(ItemStack stack) {
        return hasMarker(stack, MARKER_PARTY);
    }

    public static boolean isHubItem(ItemStack stack) {
        return isQueueSword(stack) || isPartyItem(stack) || RankedSelectorItem.isSelector(stack);
    }

    private static boolean hasMarker(ItemStack stack, String marker) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        List<String> lore = stack.getItemMeta().getLore();
        if (lore == null) {
            return false;
        }
        return lore.stream().anyMatch(line -> marker.equals(line));
    }
}

package com.obm.network.tierspace.ui;

import com.obm.network.tierspace.hub.TierSpaceHubKeys;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class RankedHubItems {

    public static final String TYPE_QUEUE = "queue";
    public static final String TYPE_PARTY = "party";
    public static final String TYPE_LEAVE = "leave";

    /** Legado — fallback se PDC ausente */
    public static final String MARKER_QUEUE = "§8obm:ts_queue";
    public static final String MARKER_PARTY = "§8obm:ts_party";
    public static final String MARKER_LEAVE = "§8obm:ts_leave";

    private RankedHubItems() {
    }

    public static ItemStack queueSword(Plugin plugin, String displayName, List<String> lore) {
        return tagged(plugin, new ItemStack(Material.IRON_SWORD), TYPE_QUEUE, displayName, lore, true);
    }

    public static ItemStack partyItem(Plugin plugin, String displayName, List<String> lore) {
        return tagged(plugin, new ItemStack(Material.PLAYER_HEAD), TYPE_PARTY, displayName, lore, false);
    }

    public static ItemStack leaveItem(Plugin plugin, String displayName, List<String> lore) {
        return tagged(plugin, new ItemStack(Material.RED_BED), TYPE_LEAVE, displayName, lore, false);
    }

    private static ItemStack tagged(Plugin plugin, ItemStack item, String type,
                                    String displayName, List<String> lore, boolean enchanted) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            if (enchanted) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.setUnbreakable(true);
            }
            if (TierSpaceHubKeys.hubItemKey() != null) {
                meta.getPersistentDataContainer().set(
                        TierSpaceHubKeys.hubItemKey(), PersistentDataType.STRING, type);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isQueueSword(ItemStack stack) {
        return isType(stack, TYPE_QUEUE);
    }

    public static boolean isPartyItem(ItemStack stack) {
        return isType(stack, TYPE_PARTY);
    }

    public static boolean isLeaveItem(ItemStack stack) {
        return isType(stack, TYPE_LEAVE);
    }

    public static boolean isHubItem(ItemStack stack) {
        return isQueueSword(stack) || isPartyItem(stack) || isLeaveItem(stack);
    }

    public static int countHubItems(Player player) {
        int n = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isHubItem(stack)) {
                n++;
            }
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (isHubItem(off)) {
            n++;
        }
        return n;
    }

    public static void stripHubItems(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isHubItem(stack)) {
                player.getInventory().setItem(i, null);
            }
        }
        if (isHubItem(player.getInventory().getItemInOffHand())) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    private static boolean isType(ItemStack stack, String expected) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (TierSpaceHubKeys.hubItemKey() != null) {
            String type = meta.getPersistentDataContainer().get(
                    TierSpaceHubKeys.hubItemKey(), PersistentDataType.STRING);
            if (expected.equals(type)) {
                return true;
            }
        }
        return legacyMarker(meta, expected);
    }

    private static boolean legacyMarker(ItemMeta meta, String expected) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            return false;
        }
        String marker = switch (expected) {
            case TYPE_QUEUE -> MARKER_QUEUE;
            case TYPE_PARTY -> MARKER_PARTY;
            case TYPE_LEAVE -> MARKER_LEAVE;
            default -> "";
        };
        return lore.stream().anyMatch(line -> marker.equals(line));
    }
}

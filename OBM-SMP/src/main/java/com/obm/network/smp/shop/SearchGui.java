package com.obm.network.smp.shop;

import com.obm.network.smp.service.ShopCatalog;
import com.obm.network.smp.shop.session.ShopSession;
import com.obm.network.smp.util.GuiItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SearchGui {

    public void beginSearch(Player player, ShopSession session) {
        session.setAwaitingSearch(true);
        player.closeInventory();
        player.sendMessage("§b§lLoja §8| §7Escreve no chat o nome do item.");
        player.sendMessage("§7Ex: §fdiamond, §7bread, §fsword");
        player.sendMessage("§7Escreve §ccancelar §7para limpar o filtro.");
    }

    public static boolean matchesSearch(Material material, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String name = GuiItems.formatMaterial(material).toLowerCase(Locale.ROOT);
        String raw = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String q = query.toLowerCase(Locale.ROOT).trim();
        return name.contains(q) || raw.contains(q);
    }

    public static List<Map.Entry<Material, Integer>> filterEntries(
            ShopCatalog catalog,
            List<String> catalogCategories,
            String searchQuery) {
        List<Map.Entry<Material, Integer>> list = new ArrayList<>();
        for (String category : catalogCategories) {
            for (Map.Entry<Material, Integer> entry : catalog.getItems(category).entrySet()) {
                if (matchesSearch(entry.getKey(), searchQuery)) {
                    list.add(entry);
                }
            }
        }
        return list;
    }

    public static ItemStack shopItemStack(
            Material material,
            int unitPrice,
            boolean blocked,
            boolean vipExclusive,
            ShopSession session,
            int unitPriceWithDiscount) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        boolean selected = session.hasSelection() && session.getSelectedMaterial() == material;
        meta.setDisplayName((selected ? "§a§l" : "§f") + GuiItems.formatMaterial(material));

        List<String> lore = new ArrayList<>();
        lore.add("§7Preço: §e" + unitPriceWithDiscount + " Money/un");
        if (unitPriceWithDiscount < unitPrice) {
            lore.add("§7Base: §8" + unitPrice + " §a(-" + (unitPrice - unitPriceWithDiscount) + ")");
        }
        if (vipExclusive) {
            lore.add("§6★ Item exclusivo VIP");
        }
        if (blocked) {
            lore.add("§cIndisponível para compra");
        } else if (selected) {
            lore.add("§8────────────");
            lore.add("§7Quantidade: §e" + session.getSelectedAmount());
            lore.add("§7Barra: §f+ / − §7ou clique no item");
            lore.add("§7Shift: §f±16");
            int total = unitPriceWithDiscount * session.getSelectedAmount();
            lore.add("§7Total: §e" + total + " Money");
            lore.add("§aConfirmar na barra inferior");
        } else {
            lore.add("§eClique para selecionar");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}

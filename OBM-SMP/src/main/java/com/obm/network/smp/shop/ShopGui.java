package com.obm.network.smp.shop;

import com.obm.network.smp.service.ShopCatalog;
import com.obm.network.smp.service.ShopService;
import com.obm.network.smp.util.GuiItems;
import com.obm.network.smp.util.GuiTitles;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShopGui {

    private final ShopCatalog catalog;
    private final ShopService shopService;

    public ShopGui(ShopCatalog catalog, ShopService shopService) {
        this.catalog = catalog;
        this.shopService = shopService;
    }

    public void openCategories(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, GuiTitles.SHOP_CATEGORIES);
        int slot = 10;
        for (String category : catalog.getCategories()) {
            if (slot > 16) {
                break;
            }
            inventory.setItem(slot++, categoryIcon(category));
        }
        inventory.setItem(22, GuiItems.named(Material.BARRIER, "§cFechar"));
        player.openInventory(inventory);
    }

    public void openCategory(Player player, String category) {
        Map<Material, Integer> items = catalog.getItems(category);
        int size = Math.min(54, ((items.size() / 9) + 1) * 9);
        size = Math.max(27, size);

        Inventory inventory = Bukkit.createInventory(null, size, GuiTitles.SHOP_CATEGORY_PREFIX + category);
        int slot = 0;
        for (Map.Entry<Material, Integer> entry : items.entrySet()) {
            if (slot >= size - 9) {
                break;
            }
            boolean blocked = catalog.isBlocked(entry.getKey());
            inventory.setItem(slot++, GuiItems.shopItem(entry.getKey(), entry.getValue(), blocked));
        }
        inventory.setItem(size - 5, GuiItems.named(Material.ARROW, "§eVoltar"));
        player.openInventory(inventory);
    }

    public void openQuantity(Player player, String category, Material material) {
        shopService.setSession(player.getUniqueId(), new ShopService.ShopSession(category, material, 1));
        Inventory inventory = Bukkit.createInventory(null, 27, GuiTitles.SHOP_QUANTITY);

        inventory.setItem(4, new ItemStack(material));
        inventory.setItem(10, quantityButton(1));
        inventory.setItem(12, quantityButton(16));
        inventory.setItem(14, quantityButton(32));
        inventory.setItem(16, quantityButton(64));
        inventory.setItem(22, GuiItems.named(Material.ARROW, "§eVoltar", "§7Categoria anterior"));
        inventory.setItem(26, GuiItems.named(Material.LIME_CONCRETE, "§aContinuar", "§7Seleciona quantidade abaixo"));

        player.openInventory(inventory);
    }

    public void openConfirm(Player player, String category, Material material, int quantity) {
        shopService.setSession(player.getUniqueId(), new ShopService.ShopSession(category, material, quantity));
        int baseUnit = catalog.getPrice(category, material);
        int unitPrice = shopService.getUnitPrice(player, category, material);
        int total = unitPrice * quantity;

        Inventory inventory = Bukkit.createInventory(null, 27, GuiTitles.SHOP_CONFIRM);
        inventory.setItem(13, new ItemStack(material, quantity));
        inventory.setItem(4, GuiItems.named(Material.GOLD_INGOT, "§eResumo",
                "§7Unitário: §f" + unitPrice + " coins" + (unitPrice < baseUnit ? " §a(-" + (baseUnit - unitPrice) + ")" : ""),
                "§7Total: §e" + total + " coins",
                "§7Saldo: §a" + com.obm.network.smp.SMPPlugin.get().getEconomyService().getBalance(player.getUniqueId()) + " coins"));
        inventory.setItem(11, GuiItems.named(Material.LIME_CONCRETE, "§aConfirmar compra",
                "§7Clica para finalizar"));
        inventory.setItem(15, GuiItems.named(Material.RED_CONCRETE, "§cCancelar", "§7Voltar à quantidade"));
        player.openInventory(inventory);
    }

    private ItemStack quantityButton(int amount) {
        return GuiItems.named(Material.PAPER, "§e" + amount + "x", "§7Clique para selecionar");
    }

    private ItemStack categoryIcon(String category) {
        Material icon = switch (category.toLowerCase(Locale.ROOT)) {
            case "blocks" -> Material.GRASS_BLOCK;
            case "food" -> Material.COOKED_BEEF;
            case "ores" -> Material.DIAMOND;
            case "armor" -> Material.IRON_CHESTPLATE;
            case "tools" -> Material.IRON_PICKAXE;
            default -> Material.CHEST;
        };
        return GuiItems.named(icon, "§e" + capitalize(category), "§7Clique para ver itens");
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}

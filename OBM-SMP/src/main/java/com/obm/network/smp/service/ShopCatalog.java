package com.obm.network.smp.service;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ShopCatalog {

    private final Map<String, Map<Material, Integer>> categories = new LinkedHashMap<>();
    private final Set<Material> blockedItems = EnumSet.noneOf(Material.class);
    private final Set<Material> vipItems = EnumSet.noneOf(Material.class);

    public void reload(FileConfiguration config) {
        categories.clear();
        blockedItems.clear();
        vipItems.clear();

        for (String materialName : config.getStringList("shop.blocked-items")) {
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
                blockedItems.add(material);
            }
        }

        for (String materialName : config.getStringList("shop.vip-items")) {
            Material material = Material.matchMaterial(materialName);
            if (material != null && !material.isAir()) {
                vipItems.add(material);
            }
        }

        ConfigurationSection section = config.getConfigurationSection("shop.categories");
        if (section == null) {
            return;
        }

        for (String category : section.getKeys(false)) {
            ConfigurationSection items = section.getConfigurationSection(category);
            if (items == null) {
                continue;
            }
            Map<Material, Integer> categoryItems = new LinkedHashMap<>();
            for (String materialName : items.getKeys(false)) {
                Material material = Material.matchMaterial(materialName);
                if (material == null || material.isAir()) {
                    continue;
                }
                categoryItems.put(material, items.getInt(materialName));
            }
            if (!categoryItems.isEmpty()) {
                categories.put(category, categoryItems);
            }
        }
    }

    public Set<String> getCategories() {
        return categories.keySet();
    }

    public Map<Material, Integer> getItems(String category) {
        return categories.getOrDefault(category, Collections.emptyMap());
    }

    public boolean isBlocked(Material material) {
        return blockedItems.contains(material);
    }

    public boolean canPurchase(Material material) {
        return !isBlocked(material);
    }

    public boolean isVipItem(Material material) {
        return vipItems.contains(material);
    }

    public int getPrice(String category, Material material) {
        return getItems(category).getOrDefault(material, 0);
    }

    public Map<Material, Integer> getAllItemsFlat() {
        Map<Material, Integer> all = new HashMap<>();
        categories.values().forEach(map -> map.forEach(all::putIfAbsent));
        return all;
    }
}

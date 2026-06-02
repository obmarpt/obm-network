package com.obm.network.smp.service;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SellCatalog {

    private final Map<Material, Integer> prices = new HashMap<>();

    public void reload(FileConfiguration config) {
        prices.clear();
        ConfigurationSection section = config.getConfigurationSection("sell.prices");
        if (section == null) {
            return;
        }
        for (String materialName : section.getKeys(false)) {
            Material material = Material.matchMaterial(materialName);
            if (material == null || material.isAir()) {
                continue;
            }
            prices.put(material, section.getInt(materialName));
        }
    }

    public int getPrice(Material material) {
        return prices.getOrDefault(material, 0);
    }

    public Map<Material, Integer> getPrices() {
        return Collections.unmodifiableMap(prices);
    }

    public boolean isSellable(Material material) {
        return prices.containsKey(material) && prices.get(material) > 0;
    }
}

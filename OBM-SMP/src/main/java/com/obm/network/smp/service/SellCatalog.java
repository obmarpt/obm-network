package com.obm.network.smp.service;

import com.obm.network.smp.util.MaterialKeys;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SellCatalog {

    private final Map<Material, Integer> prices = new HashMap<>();

    public void reload(JavaPlugin plugin, FileConfiguration config) {
        prices.clear();
        ConfigurationSection section = config.getConfigurationSection("sell.prices");
        if (section == null) {
            plugin.getLogger().warning("sell.prices em falta no config.yml — nenhum item vendável.");
            return;
        }

        int skipped = 0;
        for (String materialName : section.getKeys(false)) {
            Material material = MaterialKeys.resolve(materialName);
            if (material == null) {
                skipped++;
                plugin.getLogger().warning("sell.prices: material inválido '" + materialName + "'");
                continue;
            }
            int price = section.getInt(materialName, 0);
            if (price > 0) {
                prices.put(material, price);
            }
        }

        applyLogVariants();
        plugin.getLogger().info("Sell catalog: " + prices.size() + " materiais"
                + (skipped > 0 ? " (" + skipped + " ignorados)" : ""));
    }

    /** Troncos/variantes comuns herdam preço do carvalho quando existir. */
    private void applyLogVariants() {
        int oakLog = prices.getOrDefault(Material.OAK_LOG, 0);
        if (oakLog <= 0) {
            return;
        }
        for (Material material : Material.values()) {
            if (!material.isBlock()) {
                continue;
            }
            String name = material.name();
            if (name.endsWith("_LOG") || name.endsWith("_WOOD")) {
                prices.putIfAbsent(material, oakLog);
            }
        }
    }

    public int getPrice(Material material) {
        if (material == null || material.isAir()) {
            return 0;
        }
        return prices.getOrDefault(material, 0);
    }

    public Map<Material, Integer> getPrices() {
        return Collections.unmodifiableMap(prices);
    }

    public boolean isSellable(Material material) {
        return getPrice(material) > 0;
    }
}

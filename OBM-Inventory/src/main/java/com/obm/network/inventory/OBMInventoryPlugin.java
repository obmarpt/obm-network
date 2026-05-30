package com.obm.network.inventory;

import org.bukkit.plugin.java.JavaPlugin;

import com.obm.network.inventory.listener.InventoryListener;
import com.obm.network.inventory.manager.InventoryManager;

public class OBMInventoryPlugin extends JavaPlugin {

    private static OBMInventoryPlugin instance;
    private InventoryManager inventoryManager;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        // ✅ criar manager
        inventoryManager = new InventoryManager();

        // ✅ registar listener
        new InventoryListener(this, inventoryManager);

        getLogger().info("✅ OBM-Inventory iniciado");
    }

    @Override
    public void onDisable() {
        getLogger().info("⛔ OBM-Inventory desligado");
    }

    public static OBMInventoryPlugin get() {
        return instance;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
}
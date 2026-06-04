package com.obm.network.inventory.manager;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import com.obm.network.core.world.WorldModeService;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryManager {

    private final Map<UUID, ItemStack[]> uhcInv = new HashMap<>();
    private final Map<UUID, ItemStack[]> smpInv = new HashMap<>();
    private final DataStore dataStore;
    private final WorldModeService wms;

    public InventoryManager() {
        this.dataStore = OBMCorePlugin.get().getDataStore();
        this.wms = OBMCorePlugin.get().getWorldModeService();
    }

    private String getMode(String world) {
        if (wms.isUHC(world)) return "uhc";
        if (wms.isSMP(world)) return "smp";
        return null;
    }

    public void saveInventory(Player p, String world) {
        String mode = getMode(world);
        if (mode == null) {
            return;
        }

        UUID uuid = p.getUniqueId();
        ItemStack[] contents = p.getInventory().getContents();
        ItemStack[] copy = clone(contents);

        switch (mode) {
            case "uhc" -> uhcInv.put(uuid, copy);
            case "smp" -> smpInv.put(uuid, copy);
        }

        dataStore.setInventory(uuid, "mode." + mode, copy);
        dataStore.save(uuid);
    }

    private static ItemStack[] clone(ItemStack[] contents) {
        if (contents == null) {
            return new ItemStack[0];
        }
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] == null ? null : contents[i].clone();
        }
        return copy;
    }

    public void loadInventory(Player p, String world) {
        String mode = getMode(world);
        if (mode == null) {
            p.getInventory().clear();
            return;
        }

        UUID uuid = p.getUniqueId();
        ItemStack[] contents = null;

        switch (mode) {
            case "uhc" -> contents = uhcInv.get(uuid);
            case "smp" -> contents = smpInv.get(uuid);
        }

        if (contents == null) {
            contents = dataStore.getInventory(uuid, "mode." + mode);
        }

        if (contents != null) {
            p.getInventory().setContents(contents);
        } else {
            p.getInventory().clear();
        }
    }

    public void giveCompass(Player player) {
        ItemStack menuItem = new ItemStack(Material.NETHER_STAR);

        ItemMeta meta = menuItem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§b§lMenu");
            menuItem.setItemMeta(meta);
        }

        player.getInventory().setItem(4, menuItem);
    }
}

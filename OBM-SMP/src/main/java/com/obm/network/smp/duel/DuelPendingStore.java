package com.obm.network.smp.duel;

import com.obm.network.smp.SMPPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class DuelPendingStore {

    private final SMPPlugin plugin;
    private final File file;
    private final Map<UUID, List<ItemStack>> pending = new ConcurrentHashMap<>();

    DuelPendingStore(SMPPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "duel-pending.yml");
        load();
    }

    void add(UUID playerId, List<ItemStack> items) {
        if (playerId == null || items == null || items.isEmpty()) {
            return;
        }
        List<ItemStack> bucket = pending.computeIfAbsent(playerId, id -> new ArrayList<>());
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                bucket.add(item.clone());
            }
        }
        saveAsync();
    }

    List<ItemStack> drain(UUID playerId) {
        List<ItemStack> items = pending.remove(playerId);
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        saveAsync();
        return items;
    }

    void saveSync() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, List<ItemStack>> entry : pending.entrySet()) {
            List<Map<String, Object>> serialized = new ArrayList<>();
            for (ItemStack item : entry.getValue()) {
                if (item != null && !item.getType().isAir()) {
                    serialized.add(item.serialize());
                }
            }
            if (!serialized.isEmpty()) {
                yaml.set("players." + entry.getKey(), serialized);
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[Duel] Falha ao guardar duel-pending.yml: " + e.getMessage());
        }
    }

    private void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveSync);
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                List<?> raw = section.getList(key);
                if (raw == null) {
                    continue;
                }
                List<ItemStack> items = new ArrayList<>();
                for (Object obj : raw) {
                    if (obj instanceof Map<?, ?> map) {
                        @SuppressWarnings("unchecked")
                        ItemStack stack = ItemStack.deserialize((Map<String, Object>) map);
                        if (stack != null && !stack.getType().isAir()) {
                            items.add(stack);
                        }
                    }
                }
                if (!items.isEmpty()) {
                    pending.put(uuid, items);
                }
            } catch (IllegalArgumentException ignored) {
                // invalid uuid key
            }
        }
    }
}

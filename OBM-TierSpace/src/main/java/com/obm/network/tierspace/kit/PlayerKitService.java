package com.obm.network.tierspace.kit;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

public final class PlayerKitService {

    private static final String PREF_KEY = "tierspace_kit_preference";
    private static final String KIT_KEY = "tierspace_custom_kit";

    private final DataStore dataStore;
    private final KitService kitService;

    public PlayerKitService(KitService kitService) {
        this.dataStore = OBMCorePlugin.get().getDataStore();
        this.kitService = kitService;
    }

    public KitPreference getPreference(UUID uuid) {
        return KitPreference.fromString(dataStore.getString(uuid, PREF_KEY, KitPreference.DEFAULT.name()));
    }

    public void setPreference(UUID uuid, KitPreference preference) {
        dataStore.set(uuid, PREF_KEY, preference.name());
        dataStore.save(uuid);
    }

    public boolean hasCustomKit(UUID uuid) {
        return dataStore.has(uuid, KIT_KEY);
    }

    public void saveCustomKit(Player player) {
        UUID uuid = player.getUniqueId();
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream serializer = new BukkitObjectOutputStream(output)) {
                serializer.writeInt(41);
                ItemStack[] contents = player.getInventory().getContents();
                for (int i = 0; i < 41; i++) {
                    serializer.writeObject(i < contents.length ? contents[i] : null);
                }
                ItemStack[] armor = player.getInventory().getArmorContents();
                for (ItemStack piece : armor) {
                    serializer.writeObject(piece);
                }
            }
            dataStore.set(uuid, KIT_KEY, Base64Coder.encodeLines(output.toByteArray()));
            dataStore.save(uuid);
        } catch (Exception e) {
            player.sendMessage("§cErro ao guardar kit.");
        }
    }

    public void applyForMatch(Player player, String defaultKitId) {
        KitPreference pref = getPreference(player.getUniqueId());
        switch (pref) {
            case CUSTOM -> {
                if (!applyCustomKit(player)) {
                    kitService.applyKit(player, defaultKitId);
                }
            }
            case RANDOM -> kitService.applyRandomKit(player);
            default -> kitService.applyKit(player, defaultKitId);
        }
    }

    private boolean applyCustomKit(Player player) {
        UUID uuid = player.getUniqueId();
        if (!dataStore.has(uuid, KIT_KEY)) {
            return false;
        }
        try {
            byte[] data = Base64Coder.decodeLines(dataStore.getString(uuid, KIT_KEY, ""));
            try (BukkitObjectInputStream deserializer = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
                int size = deserializer.readInt();
                ItemStack[] contents = new ItemStack[size];
                for (int i = 0; i < size; i++) {
                    contents[i] = (ItemStack) deserializer.readObject();
                }
                player.getInventory().setContents(contents);
                ItemStack[] armor = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    armor[i] = (ItemStack) deserializer.readObject();
                }
                player.getInventory().setArmorContents(armor);
            }
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

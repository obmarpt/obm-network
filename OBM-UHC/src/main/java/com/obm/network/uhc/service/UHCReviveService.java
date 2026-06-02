package com.obm.network.uhc.service;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.location.SafeSpawnService;
import com.obm.network.core.storage.DataStore;
import com.obm.network.uhc.OBMUHCPlugin;
import com.obm.network.uhc.util.UHCUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class UHCReviveService {

    public enum ReviveType {
        NORMAL,
        TOTAL
    }

    private static final String PENDING_REVIVE_KEY = "uhc_pending_revive";

    private final OBMUHCPlugin plugin;
    private final DataStore dataStore;

    public UHCReviveService(OBMUHCPlugin plugin) {
        this.plugin = plugin;
        this.dataStore = OBMCorePlugin.get().getDataStore();
    }

    public Location getSafeSpawn() {
        String uhcWorldName = OBMCorePlugin.get().getWorldModeService().getPrimaryUHCWorld();
        World world = Bukkit.getWorld(uhcWorldName);
        return SafeSpawnService.getSafeSpawn(world);
    }

    public void prepareReviveLocation(UUID uuid) {
        Location safeSpawn = getSafeSpawn();
        if (safeSpawn != null) {
            dataStore.saveGameModeLocation(uuid, "uhc", safeSpawn);
            dataStore.save(uuid);
        }
    }

    public int reviveAllEliminated() {
        int count = 0;
        var playersSection = dataStore.getYaml().getConfigurationSection("players");
        if (playersSection == null) {
            return 0;
        }

        for (String uuidStr : playersSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            if (dataStore.getInt(uuid, "lives_uhc") <= 0) {
                scheduleRevive(uuid, ReviveType.NORMAL);
                count++;
            }
        }

        dataStore.save();
        return count;
    }

    public void scheduleRevive(UUID uuid, ReviveType type) {
        dataStore.set(uuid, "lives_uhc", 1);
        dataStore.getYaml().set("players." + uuid + "." + PENDING_REVIVE_KEY, type.name());
        prepareReviveLocation(uuid);
        dataStore.save(uuid);
    }

    public void processPendingRevive(Player player) {
        UUID uuid = player.getUniqueId();
        String pending = dataStore.getYaml().getString("players." + uuid + "." + PENDING_REVIVE_KEY);
        if (pending == null) {
            return;
        }

        ReviveType type;
        try {
            type = ReviveType.valueOf(pending);
        } catch (IllegalArgumentException ex) {
            dataStore.getYaml().set("players." + uuid + "." + PENDING_REVIVE_KEY, null);
            dataStore.save(uuid);
            return;
        }
        dataStore.getYaml().set("players." + uuid + "." + PENDING_REVIVE_KEY, null);
        dataStore.save(uuid);

        Location safeSpawn = getSafeSpawn();
        if (safeSpawn != null) {
            dataStore.saveGameModeLocation(uuid, "uhc", safeSpawn);
            dataStore.save(uuid);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> applyRevive(player, type, safeSpawn), 15L);
    }

    public void applyRevive(Player player, ReviveType type, Location safeSpawn) {
        if (!player.isOnline() || !UHCUtils.isUHC(player)) {
            return;
        }

        if (safeSpawn != null) {
            player.teleport(safeSpawn);
        }

        restoreLives(player);

        if (type == ReviveType.TOTAL) {
            restoreInventory(player);
        }
    }

    public void restoreLives(Player player) {
        UHCUtils.giveFullLives(player);
        player.setGameMode(GameMode.SURVIVAL);
    }

    public void restoreInventory(Player player) {
        UUID uuid = player.getUniqueId();
        var contents = dataStore.getInventory(uuid, "saved_inventory");
        if (contents == null) {
            return;
        }

        player.getInventory().setContents(contents);
        dataStore.getYaml().set("players." + uuid + ".saved_inventory", null);
        dataStore.save(uuid);
    }

    public void saveSafeSpawnForEliminated(UUID uuid) {
        Location safeSpawn = getSafeSpawn();
        if (safeSpawn != null) {
            dataStore.saveGameModeLocation(uuid, "uhc", safeSpawn);
        }
    }
}

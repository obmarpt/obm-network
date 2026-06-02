package com.obm.network.uhc.manager;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import com.obm.network.uhc.service.UHCReviveService;
import com.obm.network.uhc.util.UHCUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UHCManager {

    private final DataStore dataStore;
    private final UHCReviveService reviveService;

    public UHCManager(UHCReviveService reviveService) {
        this.dataStore = OBMCorePlugin.get().getDataStore();
        this.reviveService = reviveService;
    }

    public boolean isUHC(Player player) {
        return OBMCorePlugin.get().getWorldModeService().isUHC(player.getWorld().getName());
    }

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();

        if (!dataStore.has(uuid, "join_time_uhc")) {
            dataStore.set(uuid, "join_time_uhc", System.currentTimeMillis());
            dataStore.set(uuid, "lives_uhc", 1);
        }
    }

    public void checkProtection(Player player) {
        UUID uuid = player.getUniqueId();

        if (dataStore.getInt(uuid, "lives_uhc") <= 0) {
            return;
        }

        if (dataStore.getLong(uuid, "join_time_uhc") <= 0) {
            dataStore.set(uuid, "join_time_uhc", System.currentTimeMillis());
        }
    }

    public void handleKill(Player killer) {
        // Com apenas 1 vida no UHC, matar não remove vidas extras.
    }

    public void handleDeath(Player victim, Player killer) {
        UUID uuid = victim.getUniqueId();

        victim.getWorld().strikeLightningEffect(victim.getLocation());
        dataStore.setInventory(uuid, "saved_inventory", victim.getInventory().getContents());

        int lives = dataStore.getInt(uuid, "lives_uhc") - 1;
        dataStore.set(uuid, "lives_uhc", lives);

        if (killer != null && isUHC(killer)) {
            handleKill(killer);
        }

        if (lives <= 0) {
            reviveService.saveSafeSpawnForEliminated(uuid);
            dataStore.setBoolean(uuid, "uhc_send_to_lobby", true);
        } else {
            dataStore.saveGameModeLocation(uuid, "uhc", victim.getLocation());
        }

        dataStore.save(uuid);
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        dataStore.saveGameModeLocation(uuid, "uhc", player.getLocation());
        dataStore.setInventory(uuid, "saved_inventory", player.getInventory().getContents());
        dataStore.save(uuid);
    }

    public boolean shouldSendToLobby(Player player) {
        return dataStore.getBoolean(player.getUniqueId(), "uhc_send_to_lobby");
    }

    public void clearSendToLobbyFlag(UUID uuid) {
        dataStore.setBoolean(uuid, "uhc_send_to_lobby", false);
        dataStore.save(uuid);
    }

    public Optional<Location> resolveLobbyRespawnLocation() {
        Plugin lobbyPlugin = Bukkit.getPluginManager().getPlugin("OBM-Lobby");
        if (!(lobbyPlugin instanceof JavaPlugin javaPlugin) || !lobbyPlugin.isEnabled()) {
            return Optional.empty();
        }

        FileConfiguration config = javaPlugin.getConfig();
        String worldName = config.getString("lobby.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }

        Location spawn = new Location(
                world,
                config.getDouble("lobby.spawn.x"),
                config.getDouble("lobby.spawn.y"),
                config.getDouble("lobby.spawn.z"),
                (float) config.getDouble("lobby.spawn.yaw"),
                (float) config.getDouble("lobby.spawn.pitch")
        );

        return Optional.of(spawn);
    }

    public void openDeathMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("MORTE UHC", NamedTextColor.DARK_RED));

        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Foste eliminado", NamedTextColor.RED));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Morreste no UHC", NamedTextColor.GRAY),
                    Component.text("Aguarda por um revive do staff!", NamedTextColor.YELLOW)
            ));
            item.setItemMeta(meta);
        }

        inventory.setItem(13, item);
        player.openInventory(inventory);
    }

    public void ensureHardcoreWorld() {
        String uhcWorld = OBMCorePlugin.get().getWorldModeService().getPrimaryUHCWorld();
        World world = Bukkit.getWorld(uhcWorld);
        if (world != null) {
            world.setHardcore(true);
        }
    }

    public int getLives(Player player) {
        return dataStore.getInt(player.getUniqueId(), "lives_uhc");
    }
}

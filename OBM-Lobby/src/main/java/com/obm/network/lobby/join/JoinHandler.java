package com.obm.network.lobby.join;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import com.obm.network.lobby.OBMLobbyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class JoinHandler implements Listener {

    private final OBMLobbyPlugin plugin;
    private final DataStore ds;
    private static final long LOGOUT_TIMEOUT = 60000L; // 1 minuto

    public JoinHandler(OBMLobbyPlugin plugin) {
        this.plugin = plugin;
        this.ds = OBMCorePlugin.get().getDataStore();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // =========================
    // JOIN
    // =========================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        event.setJoinMessage(null);

        String lobbyWorld = plugin.getConfig().getString("lobby.world");
        if (lobbyWorld == null)
            return;

        if (!player.getWorld().getName().equalsIgnoreCase(lobbyWorld))
            return;

        resetPlayer(player);
        teleportToLobby(player);

        // ✅ FORÇA inventário DEPOIS de tudo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.getInventory().clear();
            giveMenu(player);
        }, 10L);
    }

    // =========================
    // WORLD CHANGE
    // =========================
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {

        Player player = e.getPlayer();

        String lobbyWorld = plugin.getConfig().getString("lobby.world");
        if (lobbyWorld == null)
            return;

        String to = player.getWorld().getName();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (to.equalsIgnoreCase(lobbyWorld)) {

                // ✅ RESET COMPLETO NO LOBBY
                resetPlayer(player);
                player.getInventory().clear();

                giveMenu(player);
            }

        }, 5L);
    }

    // =========================
    // RESET PLAYER
    // =========================
    private void resetPlayer(Player player) {

        player.setFireTicks(0);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);

        player.setLevel(0);
        player.setExp(0f);

        player.getActivePotionEffects()
                .forEach(effect -> player.removePotionEffect(effect.getType()));
    }

    // =========================
    // RESET EFFECTS ONLY
    // =========================
    private void resetPlayerEffects(Player player) {

        player.setFireTicks(0);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);

        player.setLevel(0);
        player.setExp(0f);

        player.getActivePotionEffects()
                .forEach(effect -> player.removePotionEffect(effect.getType()));
    }

    // =========================
    // TELEPORT
    // =========================
    private void teleportToLobby(Player player) {

        String worldName = plugin.getConfig().getString("lobby.world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("❌ Lobby world not found: " + worldName);
            return;
        }

        Location spawn = new Location(
                world,
                plugin.getConfig().getDouble("lobby.spawn.x"),
                plugin.getConfig().getDouble("lobby.spawn.y"),
                plugin.getConfig().getDouble("lobby.spawn.z"),
                (float) plugin.getConfig().getDouble("lobby.spawn.yaw"),
                (float) plugin.getConfig().getDouble("lobby.spawn.pitch"));

        player.teleport(spawn);
    }

    // =========================
    // MENU ITEM
    // =========================
    private void giveMenu(Player player) {

        player.getInventory().remove(Material.NETHER_STAR);

        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§b§lMenu");
            item.setItemMeta(meta);
        }

        player.getInventory().setItem(4, item);
    }

    // =========================
    // PROTECTIONS
    // =========================
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {

        String lobbyWorld = plugin.getConfig().getString("lobby.world");
        if (lobbyWorld == null)
            return;

        if (e.getPlayer().getWorld().getName().equalsIgnoreCase(lobbyWorld)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {

        if (!(e.getEntity() instanceof Player player))
            return;

        String lobbyWorld = plugin.getConfig().getString("lobby.world");
        if (lobbyWorld == null)
            return;

        if (player.getWorld().getName().equalsIgnoreCase(lobbyWorld)) {
            e.setCancelled(true);
        }
    }
}
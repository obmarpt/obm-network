package com.obm.network.uhc.listeners;

import com.obm.network.uhc.manager.UHCManager;
import com.obm.network.uhc.util.UHCUtils;
import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import com.obm.network.uhc.OBMUHCPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.*;import org.bukkit.configuration.file.FileConfiguration;import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class UHCListener implements Listener {

    private final UHCManager manager = new UHCManager();
    private static final DataStore ds = OBMCorePlugin.get().getDataStore();

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if (!UHCUtils.isUHC(p)) return;

        manager.handleJoin(p);

        // Ensure UHC world is hardcore when a UHC player joins.
        ensureHardcoreUHC();
    }

    private void ensureHardcoreUHC() {
        try {
            String uhcWorld = OBMCorePlugin.get().getWorldModeService().getPrimaryUHCWorld();
            World w = Bukkit.getWorld(uhcWorld);
            if (w != null) {
                w.setHardcore(true);
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        if (!UHCUtils.isUHC(p)) return;

        UUID uuid = p.getUniqueId();

        // Salva a localização e inventário do UHC no formato de modos
        ds.saveGameModeLocation(uuid, "uhc", p.getLocation());
        ds.setInventory(uuid, "saved_inventory", p.getInventory().getContents());
        ds.save(uuid);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        if (!UHCUtils.isUHC(p)) return;

        UUID uuid = p.getUniqueId();

        // ⚡ Efeito visual de raio
        p.getWorld().strikeLightningEffect(p.getLocation());

        // 🚀 Salva a última posição UHC antes de morrer
        ds.saveGameModeLocation(uuid, "uhc", p.getLocation());

        // 🚀 Salva o inventário da morte e a localização no DataStore
        ds.setInventory(uuid, "saved_inventory", p.getInventory().getContents());
        ds.setLocation(uuid, "last_death_location", p.getLocation());
        ds.save(uuid);

        // ✅ Reduzir vidas no Manager
        manager.handleDeath(p);

        // Processar Kills
        Player killer = p.getKiller();
        if (killer != null && manager.isUHC(killer)) {
            manager.handleKill(killer);
        }
        // If player now has no lives left, mark to send to lobby on respawn
        if (UHCUtils.getLives(p) <= 0) {
            ds.setBoolean(uuid, "uhc_send_to_lobby", true);
            ds.save(uuid);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        
        if (!UHCUtils.isUHC(player)) return;
        
        boolean sendToLobby = ds.getBoolean(player.getUniqueId(), "uhc_send_to_lobby");
        if (sendToLobby) {
            ds.setBoolean(player.getUniqueId(), "uhc_send_to_lobby", false);
            ds.save(player.getUniqueId());
            Plugin lobbyPlugin = Bukkit.getPluginManager().getPlugin("OBM-Lobby");
            if (!(lobbyPlugin instanceof JavaPlugin) || !lobbyPlugin.isEnabled()) {
                player.sendMessage(Component.text("Erro: plugin OBM-Lobby não está disponível!", NamedTextColor.RED));
                return;
            }

            FileConfiguration config = ((JavaPlugin) lobbyPlugin).getConfig();
            String worldName = config.getString("lobby.world", "world");
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                player.sendMessage(Component.text("Erro: mundo do Lobby não foi encontrado!", NamedTextColor.RED));
                return;
            }

            double x = config.getDouble("lobby.spawn.x");
            double y = config.getDouble("lobby.spawn.y");
            double z = config.getDouble("lobby.spawn.z");
            float yaw = (float) config.getDouble("lobby.spawn.yaw");
            float pitch = (float) config.getDouble("lobby.spawn.pitch");

            Location spawn = new Location(world, x, y, z, yaw, pitch);
            e.setRespawnLocation(spawn);

            Bukkit.getScheduler().runTaskLater(OBMUHCPlugin.get(), () -> {
                if (player.isOnline()) {
                    openDeathMenu(player);
                }
            }, 10L);
        }
    }

    private void openDeathMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("MORTE UHC", NamedTextColor.DARK_RED));

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
        inv.setItem(13, item);
        p.openInventory(inv);
    }

    // 🚀 Restaurar inventário lendo direto do DataStore
    public static void restoreInventory(Player p) {
        ItemStack[] contents = ds.getInventory(p.getUniqueId(), "saved_inventory");
        if (contents != null) {
            p.getInventory().setContents(contents);
            // Opcional: deletar do banco após restaurar para limpar espaço do ficheiro yml
            ds.getYaml().set("players." + p.getUniqueId() + ".saved_inventory", null);
            ds.save(p.getUniqueId());
        }
    }

    public static void reviveOnly(Player p) {
        UHCUtils.giveFullLives(p);
        p.setGameMode(GameMode.SURVIVAL);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().title().equals(Component.text("MORTE UHC", NamedTextColor.DARK_RED))) {
            e.setCancelled(true);
        }
    }
}
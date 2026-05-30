package com.obm.network.lobby.gui;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.combat.CooldownService;
import com.obm.network.core.storage.DataStore;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MenuManager implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        String title = e.getView().getTitle();
        ItemStack item = e.getCurrentItem();

        if (item == null || item.getType() == Material.AIR) return;

        DataStore ds = OBMCorePlugin.get().getDataStore();

        if (title.equalsIgnoreCase("§6§lOBM NETWORK")) {
            e.setCancelled(true);
            switch (item.getType()) {
                case GRASS_BLOCK -> player.openInventory(SMPMenu.create(player));
                case DIAMOND_SWORD -> player.openInventory(UHCMenu.create(player));
                case BLAZE_POWDER -> player.openInventory(RushMenu.create(player));
                case PLAYER_HEAD -> player.openInventory(ProfileMenu.create(player));
                default -> {}
            }
            return;
        }

        if (title.equalsIgnoreCase("§a§lSMP")) {
            e.setCancelled(true);
            if (item.getType() == Material.COMPASS) {
                World world = Bukkit.getWorld(OBMCorePlugin.get().getWorldModeService().getPrimarySMPWorld());
                if (world == null) {
                    player.sendMessage("§cErro: mundo SMP não encontrado!");
                    return;
                }

                player.teleport(world.getSpawnLocation());
                player.sendMessage("§aBem-vindo ao SMP! Divirta-te.");
                CooldownService.setEnteredMode(player);
                player.closeInventory();
            } else if (item.getType() == Material.ARROW) {
                player.openInventory(MainMenu.create());
            }
            return;
        }

        if (title.equalsIgnoreCase("§c§lRUSH SMP")) {
            e.setCancelled(true);
            if (item.getType() == Material.IRON_SWORD) {
                String rushWorld = OBMCorePlugin.get().getWorldModeService().getPrimaryRushWorld();
                World world = Bukkit.getWorld(rushWorld);
                if (world == null) {
                    player.sendMessage("§cErro: mundo Rush não encontrado!");
                    return;
                }
                String lockKey = "location_restore_locked_" + player.getUniqueId();
                DataStore menuStore = OBMCorePlugin.get().getDataStore();
                
                player.teleport(world.getSpawnLocation());
                // Reset player state for Rush
                try {
                    player.setHealth(player.getMaxHealth());
                } catch (Exception ignored) {}
                player.setFoodLevel(20);
                player.setSaturation(20f);
                for (org.bukkit.potion.PotionEffect pe : player.getActivePotionEffects()) {
                    player.removePotionEffect(pe.getType());
                }
                player.sendMessage("§aTeleportando para Rush SMP: §e" + rushWorld);
                CooldownService.setEnteredMode(player);
                player.closeInventory();
            } else if (item.getType() == Material.GOLDEN_APPLE) {
                // open GUI shop for Rush
                try {
                    player.openInventory(Class.forName("com.obm.network.smp.gui.RushShopMenu").asSubclass(Object.class)
                            .getMethod("create", org.bukkit.entity.Player.class)
                            .invoke(null, player) instanceof org.bukkit.inventory.Inventory inv ? inv : null);
                } catch (Exception ex) {
                    // fallback to command if GUI not available
                    player.performCommand("shop list");
                }
                player.closeInventory();
            } else if (item.getType() == Material.ARROW) {
                player.openInventory(MainMenu.create());
            }
            return;
        }

        if (title.equalsIgnoreCase("§c§lUHC")) {
            e.setCancelled(true);
            if (item.getType() == Material.IRON_SWORD) {
                int lives = ds.getInt(player.getUniqueId(), "lives_uhc");
                if (lives <= 0) {
                    player.sendMessage("§cNão tens vidas no UHC!");
                    player.closeInventory();
                    return;
                }
                World world = Bukkit.getWorld(OBMCorePlugin.get().getWorldModeService().getPrimaryUHCWorld());
                if (world == null) {
                    player.sendMessage("§cErro: mundo UHC não encontrado!");
                    return;
                }
                String lockKey = "location_restore_locked_" + player.getUniqueId();
                DataStore menuStore = OBMCorePlugin.get().getDataStore();
                
                player.teleport(world.getSpawnLocation());
                // Reset player state for UHC
                try {
                    player.setHealth(player.getMaxHealth());
                } catch (Exception ignored) {}
                player.setFoodLevel(20);
                player.setSaturation(20f);
                for (org.bukkit.potion.PotionEffect pe : player.getActivePotionEffects()) {
                    player.removePotionEffect(pe.getType());
                }
                player.sendMessage("§aBem-vindo ao UHC! Boa sorte.");
                CooldownService.setEnteredMode(player);
                player.closeInventory();
            } else if (item.getType() == Material.ARROW) {
                player.openInventory(MainMenu.create());
            }
            return;
        }

        if (title.equalsIgnoreCase("§e§lPROFILE")) {
            e.setCancelled(true);
            if (item.getType() == Material.ARROW) {
                player.openInventory(MainMenu.create());
            }
        }
    }
}
package com.obm.network.lobby.gui;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.combat.CooldownService;
import com.obm.network.core.location.SafeSpawnService;
import com.obm.network.core.storage.DataStore;
import com.obm.network.smp.SMPPlugin;
import com.obm.network.smp.shop.ShopGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MenuManager implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        ItemStack item = event.getCurrentItem();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        DataStore ds = OBMCorePlugin.get().getDataStore();

        if (title.equalsIgnoreCase("§6§lOBM NETWORK")) {
            event.setCancelled(true);
            switch (item.getType()) {
                case GRASS_BLOCK -> player.openInventory(SMPMenu.create(player));
                case DIAMOND_SWORD -> player.openInventory(UHCMenu.create(player));
                case PLAYER_HEAD -> player.openInventory(ProfileMenu.create(player));
                default -> {}
            }
            return;
        }

        if (title.equalsIgnoreCase("§a§lSMP")) {
            event.setCancelled(true);
            if (item.getType() == Material.COMPASS) {
                World world = Bukkit.getWorld(OBMCorePlugin.get().getWorldModeService().getPrimarySMPWorld());
                if (world == null) {
                    player.sendMessage("§cErro: mundo SMP não encontrado!");
                    return;
                }

                String lockKey = "location_restore_locked_" + player.getUniqueId();
                ds.setBoolean(player.getUniqueId(), lockKey, true);
                ds.save(player.getUniqueId());

                var safeSpawn = SafeSpawnService.getSafeSpawn(world);
                player.teleport(safeSpawn != null ? safeSpawn : world.getSpawnLocation());
                resetPlayerState(player);
                player.sendMessage("§aBem-vindo ao SMP! Divirta-te.");
                CooldownService.setEnteredMode(player);
                player.closeInventory();
            } else if (item.getType() == Material.GOLDEN_APPLE) {
                ShopGui shopGui = SMPPlugin.get().getShopGui();
                if (shopGui != null) {
                    shopGui.openCategories(player);
                }
            } else if (item.getType() == Material.CHEST) {
                var auctionGui = SMPPlugin.get().getAuctionGui();
                if (auctionGui != null) {
                    auctionGui.openMain(player);
                }
            } else if (item.getType() == Material.ARROW) {
                player.openInventory(MainMenu.create());
            }
            return;
        }

        if (title.equalsIgnoreCase("§c§lUHC")) {
            event.setCancelled(true);
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
                ds.setBoolean(player.getUniqueId(), lockKey, true);
                ds.save(player.getUniqueId());

                var safeSpawn = SafeSpawnService.getSafeSpawn(world);
                player.teleport(safeSpawn != null ? safeSpawn : world.getSpawnLocation());
                resetPlayerState(player);
                player.sendMessage("§aBem-vindo ao UHC! Boa sorte.");
                CooldownService.setEnteredMode(player);
                player.closeInventory();
            } else if (item.getType() == Material.ARROW) {
                player.openInventory(MainMenu.create());
            }
            return;
        }

        if (title.equalsIgnoreCase("§e§lPROFILE")) {
            event.setCancelled(true);
            if (item.getType() == Material.ARROW) {
                player.openInventory(MainMenu.create());
            }
        }
    }

    private void resetPlayerState(Player player) {
        try {
            player.setHealth(player.getMaxHealth());
        } catch (Exception ignored) {
        }
        player.setFoodLevel(20);
        player.setSaturation(20f);
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }
}

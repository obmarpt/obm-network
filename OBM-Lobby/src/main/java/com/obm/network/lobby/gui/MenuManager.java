package com.obm.network.lobby.gui;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.combat.CooldownService;
import com.obm.network.core.hardcore.HardcoreUnlockService;
import com.obm.network.core.location.SafeSpawnService;
import com.obm.network.core.storage.DataStore;
import com.obm.network.smp.SMPPlugin;
import com.obm.network.tierspace.TierSpacePlugin;
import com.obm.network.smp.shop.ShopGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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

        if (title.equals(MenuTitles.MAIN)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == MainMenu.SLOT_RUSH) {
                playClick(player);
                player.openInventory(SMPMenu.create(player));
            } else if (slot == MainMenu.SLOT_TIERSPACE) {
                playClick(player);
                openTierSpaceMenu(player);
            } else if (slot == MainMenu.SLOT_HARDCORE) {
                playClick(player);
                player.openInventory(UHCMenu.create(player));
            } else if (slot == MainMenu.SLOT_PROFILE) {
                playClick(player);
                player.openInventory(ProfileMenu.create(player));
            }
            return;
        }

        if (title.equals(MenuTitles.RUSH)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == SMPMenu.SLOT_PLAY) {
                enterMode(player, ds, OBMCorePlugin.get().getWorldModeService().getPrimarySMPWorld(),
                        MenuColors.rush("Bem-vindo ao Rush! Divirta-te."));
            } else if (slot == SMPMenu.SLOT_SHOP) {
                playClick(player);
                ShopGui shopGui = SMPPlugin.get().getShopGui();
                if (shopGui != null) {
                    shopGui.openCategories(player);
                }
            } else if (slot == SMPMenu.SLOT_AUCTION) {
                playClick(player);
                var auctionGui = SMPPlugin.get().getAuctionGui();
                if (auctionGui != null) {
                    auctionGui.openMain(player);
                }
            } else if (slot == SMPMenu.SLOT_BACK) {
                playClick(player);
                player.openInventory(MainMenu.create(player));
            }
            return;
        }

        if (title.equals(MenuTitles.HARDCORE)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == UHCMenu.SLOT_PLAY) {
                if (!HardcoreUnlockService.canEnter(player.getUniqueId())) {
                    playClick(player);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
                    HardcoreUnlockService.sendBlockedMessage(player);
                    return;
                }
                int lives = ds.getInt(player.getUniqueId(), "lives_uhc");
                if (lives <= 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
                    player.sendMessage(MenuColors.error("Não tens vidas no Hardcore!"));
                    player.closeInventory();
                    return;
                }
                enterMode(player, ds, OBMCorePlugin.get().getWorldModeService().getPrimaryUHCWorld(),
                        MenuColors.hardcore("Bem-vindo ao Hardcore! Boa sorte."));
            } else if (slot == UHCMenu.SLOT_BACK) {
                playClick(player);
                player.openInventory(MainMenu.create(player));
            }
            return;
        }

        if (title.equals(MenuTitles.PROFILE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == ProfileMenu.SLOT_BACK) {
                playClick(player);
                player.openInventory(MainMenu.create(player));
            }
        }
    }

    private void enterMode(Player player, DataStore ds, String worldName, String welcomeMessage) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(MenuColors.error("Erro: mundo não encontrado!"));
            return;
        }

        String lockKey = "location_restore_locked_" + player.getUniqueId();
        ds.setBoolean(player.getUniqueId(), lockKey, true);
        ds.save(player.getUniqueId());

        var safeSpawn = SafeSpawnService.getSafeSpawn(world);
        player.teleport(safeSpawn != null ? safeSpawn : world.getSpawnLocation());
        resetPlayerState(player);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);
        player.sendMessage(welcomeMessage);
        CooldownService.setEnteredMode(player);
        player.closeInventory();
    }

    private void openTierSpaceMenu(Player player) {
        TierSpacePlugin tierSpace = TierSpacePlugin.get();
        if (tierSpace == null || tierSpace.getTierGuiMenu() == null) {
            player.sendMessage(MenuColors.error("TierSpace não está disponível."));
            return;
        }
        player.openInventory(tierSpace.getTierGuiMenu().create(player));
    }

    private void playClick(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
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

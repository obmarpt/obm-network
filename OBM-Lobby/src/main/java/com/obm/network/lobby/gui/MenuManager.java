package com.obm.network.lobby.gui;



import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.permission.ObmPermissions;
import com.obm.network.core.ui.PlayerUx;
import com.obm.network.core.ui.VisualFeedback;
import com.obm.network.core.ui.VisualFeedbackScene;
import com.obm.network.core.state.PlayerStateBridge;
import com.obm.network.smp.permission.SmpPermissions;

import com.obm.network.core.combat.CooldownService;

import com.obm.network.core.hardcore.HardcoreUnlockService;

import com.obm.network.core.location.RankedHubService;

import com.obm.network.core.location.SafeSpawnService;

import com.obm.network.core.npc.NpcModeRouter;

import com.obm.network.core.storage.DataStore;

import com.obm.network.smp.SMPPlugin;

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



        if (isFiller(item)) {

            if (title.equals(MainMenuConfig.title())) {

                event.setCancelled(true);

            }

            return;

        }



        DataStore ds = OBMCorePlugin.get().getDataStore();



        if (title.equals(MainMenuConfig.title())) {

            event.setCancelled(true);

            handleMainMenuClick(player, event.getRawSlot(), ds);

            return;

        }



        if (title.equals(MenuTitles.RUSH)) {

            event.setCancelled(true);

            handleRushMenuClick(player, event.getRawSlot(), ds);

            return;

        }



        if (title.equals(MenuTitles.HARDCORE)) {

            event.setCancelled(true);

            handleHardcoreMenuClick(player, event.getRawSlot(), ds);

            return;

        }



        if (title.equals(MenuTitles.PROFILE)) {

            event.setCancelled(true);

            if (event.getRawSlot() == ProfileMenu.SLOT_BACK) {

                playClick(player);

                player.closeInventory();

            }

        }

    }



    private void handleMainMenuClick(Player player, int slot, DataStore ds) {

        if (slot == MainMenu.slotSmp()) {

            enterSmp(player, ds);

        } else if (slot == MainMenu.slotHardcore()) {

            enterHardcore(player, ds);

        } else if (slot == MainMenu.slotTierSpace()) {

            enterTierSpace(player);

        }

    }



    private void enterTierSpace(Player player) {

        playClick(player);

        if (!player.hasPermission(ObmPermissions.TIERSPACE_USE)) {
            PlayerUx.error(player, "Não tens permissão para TierSpace.");
            PlayerUx.errorSound(player);
            return;
        }

        if (!RankedHubService.teleport(player)) {

            player.sendMessage(MenuColors.error("TierSpace indisponível."));

            return;

        }

        resetPlayerState(player);

        showEnteringFeedback(player, VisualFeedbackScene.MODE_TIERSPACE, MainMenuConfig.tierSpaceItem().enteringMessage());

        OBMCorePlugin core = OBMCorePlugin.get();
        if (core != null) {
            Bukkit.getScheduler().runTaskLater(core, () -> PlayerStateBridge.syncFromWorld(player), 3L);
        }

    }



    private void enterSmp(Player player, DataStore ds) {

        playClick(player);

        if (SmpPermissions.deny(player, SmpPermissions.ENTER, "Permissão: obm.smp.enter")) {
            return;
        }

        NpcModeRouter.joinRush(player);

        player.closeInventory();

    }



    private void enterHardcore(Player player, DataStore ds) {

        playClick(player);



        if (!HardcoreUnlockService.canEnter(player.getUniqueId())) {

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);

            HardcoreUnlockService.sendBlockedMessage(player);

            return;

        }



        int lives = com.obm.network.core.integration.HardcoreStatsBridge.getLives(player.getUniqueId());

        if (lives <= 0) {

            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);

            player.sendMessage(MenuColors.error("Não tens vidas no Hardcore!"));

            player.closeInventory();

            return;

        }



        NpcModeRouter.handleHardcore(player);

        player.closeInventory();

    }



    private void teleportToModeWorld(Player player, DataStore ds, String worldName, String enteringMessage) {

        World world = Bukkit.getWorld(worldName);

        if (world == null) {

            player.sendMessage(MenuColors.error("Mundo não encontrado."));

            return;

        }



        String lockKey = "location_restore_locked_" + player.getUniqueId();

        ds.setBoolean(player.getUniqueId(), lockKey, true);

        ds.save(player.getUniqueId());



        var safeSpawn = SafeSpawnService.getSafeSpawn(world);

        player.teleport(safeSpawn != null ? safeSpawn : world.getSpawnLocation());

        resetPlayerState(player);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);

        CooldownService.setEnteredMode(player);

        showEnteringFeedback(player, VisualFeedbackScene.MODE_SMP, enteringMessage);

    }



    private void showEnteringFeedback(Player player, VisualFeedbackScene scene, String message) {

        player.closeInventory();

        String colored = MenuText.colorize(message);

        player.sendMessage(colored);

        if (MainMenuConfig.useTitleFeedback() && scene != null) {
            VisualFeedback.play(player, scene, colored);
        }

    }



    private void handleRushMenuClick(Player player, int slot, DataStore ds) {

        if (slot == SMPMenu.SLOT_PLAY) {

            enterSmp(player, ds);

        } else if (slot == SMPMenu.SLOT_SHOP) {

            playClick(player);

            if (SmpPermissions.deny(player, SmpPermissions.SHOP, "Permissão: obm.smp.shop")) {
                return;
            }

            ShopGui shopGui = SMPPlugin.get().getShopGui();

            if (shopGui != null) {

                shopGui.openCategories(player);

            }

        } else if (slot == SMPMenu.SLOT_AUCTION) {

            playClick(player);

            if (SmpPermissions.deny(player, SmpPermissions.AUCTION, "Permissão: obm.smp.auction")) {
                return;
            }

            var auctionGui = SMPPlugin.get().getAuctionGui();

            if (auctionGui != null) {

                auctionGui.openMain(player);

            }

        } else if (slot == SMPMenu.SLOT_BACK) {

            playClick(player);

            player.openInventory(MainMenu.create(player));

        }

    }



    private void handleHardcoreMenuClick(Player player, int slot, DataStore ds) {

        if (slot == UHCMenu.SLOT_PLAY) {

            enterHardcore(player, ds);

        } else if (slot == UHCMenu.SLOT_BACK) {

            playClick(player);

            player.openInventory(MainMenu.create(player));

        }

    }



    private boolean isFiller(ItemStack item) {

        Material mat = item.getType();

        return mat.name().endsWith("STAINED_GLASS_PANE") || mat == Material.BLACK_STAINED_GLASS_PANE;

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


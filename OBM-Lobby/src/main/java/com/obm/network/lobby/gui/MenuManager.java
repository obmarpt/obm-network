package com.obm.network.lobby.gui;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.combat.CooldownService;
import com.obm.network.core.hardcore.HardcoreUnlockService;
import com.obm.network.core.location.RankedHubService;
import com.obm.network.core.location.SafeSpawnService;
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
                player.openInventory(MainMenu.create(player));
            }
        }
    }

    private void handleMainMenuClick(Player player, int slot, DataStore ds) {
        if (slot == MainMenu.slotPvp()) {
            enterPvp(player);
        } else if (slot == MainMenu.slotSmp()) {
            enterSmp(player, ds);
        } else if (slot == MainMenu.slotHardcore()) {
            enterHardcore(player, ds);
        }
    }

    private void enterPvp(Player player) {
        playClick(player);
        if (!RankedHubService.teleport(player)) {
            player.sendMessage(MenuColors.error("Hub PvP indisponível."));
            return;
        }
        resetPlayerState(player);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);
        showEnteringFeedback(player, MainMenuConfig.pvpItem().enteringMessage());
    }

    private void enterSmp(Player player, DataStore ds) {
        playClick(player);
        String worldName = OBMCorePlugin.get().getWorldModeService().getPrimarySMPWorld();
        teleportToModeWorld(player, ds, worldName, MainMenuConfig.smpItem().enteringMessage());
    }

    private void enterHardcore(Player player, DataStore ds) {
        playClick(player);

        if (!HardcoreUnlockService.canEnter(player.getUniqueId())) {
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

        String worldName = OBMCorePlugin.get().getWorldModeService().getPrimaryUHCWorld();
        teleportToModeWorld(player, ds, worldName, MainMenuConfig.hardcoreItems().enteringMessage());
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
        showEnteringFeedback(player, enteringMessage);
    }

    private void showEnteringFeedback(Player player, String message) {
        player.closeInventory();
        String colored = MenuText.colorize(message);
        player.sendMessage(colored);

        if (MainMenuConfig.useTitleFeedback()) {
            player.sendTitle(
                    MenuText.colorize("&a&l»"),
                    colored,
                    MainMenuConfig.titleFadeIn(),
                    MainMenuConfig.titleStay(),
                    MainMenuConfig.titleFadeOut()
            );
        }
    }

    private void handleRushMenuClick(Player player, int slot, DataStore ds) {
        if (slot == SMPMenu.SLOT_PLAY) {
            enterSmp(player, ds);
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

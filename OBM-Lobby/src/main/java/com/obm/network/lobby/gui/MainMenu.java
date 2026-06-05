package com.obm.network.lobby.gui;

import com.obm.network.core.hardcore.HardcoreUnlockService;
import com.obm.network.core.hardcore.HardcoreUnlockStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * Menu principal — 27 slots, linha central: [vidro][PvP][Hardcore][SMP][vidro].
 * Sem estatísticas; apenas teleporte para modos.
 */
public final class MainMenu {

    public static final int SIZE = 27;

    private MainMenu() {
    }

    public static int slotPvp() {
        return MainMenuConfig.slotPvp();
    }

    public static int slotHardcore() {
        return MainMenuConfig.slotHardcore();
    }

    public static int slotSmp() {
        return MainMenuConfig.slotSmp();
    }

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, MainMenuConfig.title());
        fillBackground(inv);

        inv.setItem(slotPvp(), buildModeItem(MainMenuConfig.pvpItem(), null));
        inv.setItem(slotSmp(), buildModeItem(MainMenuConfig.smpItem(), null));
        inv.setItem(slotHardcore(), buildHardcoreItem(player.getUniqueId()));

        return inv;
    }

    private static void fillBackground(Inventory inv) {
        ItemStack filler = fillerPane();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private static ItemStack fillerPane() {
        ItemStack item = new ItemStack(MainMenuConfig.fillerMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MainMenuConfig.fillerName());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildModeItem(MainMenuConfig.ModeItem mode, java.util.Map<String, String> placeholders) {
        return MenuUtil.item(
                mode.material(),
                MenuText.applyPlaceholders(mode.name(), placeholders),
                MenuText.applyPlaceholders(mode.lore(), placeholders)
        );
    }

    private static ItemStack buildHardcoreItem(UUID uuid) {
        HardcoreUnlockStatus unlock = HardcoreUnlockService.evaluate(uuid);
        MainMenuConfig.HardcoreItems items = MainMenuConfig.hardcoreItems();

        boolean showLocked = unlock.gateEnabled() && !unlock.unlocked();
        if (showLocked) {
            var ph = MainMenuConfig.hardcorePlaceholders(unlock.requiredCoins(), unlock.requiredLevel());
            return MenuUtil.item(
                    items.lockedMaterial(),
                    MenuText.applyPlaceholders(items.lockedName(), ph),
                    MenuText.applyPlaceholders(items.lockedLore(), ph)
            );
        }

        return MenuUtil.item(
                items.unlockedMaterial(),
                MenuText.colorize(items.unlockedName()),
                MenuText.colorizeLore(items.unlockedLore())
        );
    }

    /** Compatibilidade com código legado. */
    public static Inventory create() {
        return Bukkit.createInventory(null, SIZE, MainMenuConfig.title());
    }
}

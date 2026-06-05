package com.obm.network.lobby.gui;

import com.obm.network.core.hardcore.HardcoreUnlockService;
import com.obm.network.core.hardcore.HardcoreUnlockStatus;
import com.obm.network.lobby.mode.ModeOnlineCounter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Navegador de modos — SMP · Hardcore · TierSpace com jogadores online e highlight.
 */
public final class MainMenu {

    public static final int SIZE = 27;

    private MainMenu() {
    }

    public static int slotSmp() {
        return MainMenuConfig.slotSmp();
    }

    public static int slotHardcore() {
        return MainMenuConfig.slotHardcore();
    }

    public static int slotTierSpace() {
        return MainMenuConfig.slotTierSpace();
    }

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, MainMenuConfig.title());
        fillBackground(inv);
        paintModeSlots(inv, player);
        return inv;
    }

    public static void refreshModeSlots(Inventory inv, Player player) {
        if (inv == null || inv.getSize() != SIZE) {
            return;
        }
        paintModeSlots(inv, player);
    }

    private static void paintModeSlots(Inventory inv, Player player) {
        ModeOnlineCounter.Mode highlight = ModeOnlineCounter.busiest();
        inv.setItem(slotSmp(), buildSmpItem(highlight == ModeOnlineCounter.Mode.SMP));
        inv.setItem(slotHardcore(), buildHardcoreItem(player.getUniqueId(), highlight == ModeOnlineCounter.Mode.HARDCORE));
        inv.setItem(slotTierSpace(), buildTierSpaceItem(highlight == ModeOnlineCounter.Mode.TIERSPACE));
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

    private static ItemStack buildSmpItem(boolean highlight) {
        MainMenuConfig.ModeItem mode = MainMenuConfig.smpItem();
        int online = ModeOnlineCounter.count(ModeOnlineCounter.Mode.SMP);
        List<String> lore = withOnlineAndHighlight(mode.lore(), online, highlight);
        return applyHighlight(MenuUtil.item(mode.material(), MenuText.colorize(mode.name()), lore), highlight);
    }

    private static ItemStack buildTierSpaceItem(boolean highlight) {
        MainMenuConfig.ModeItem mode = MainMenuConfig.tierSpaceItem();
        int online = ModeOnlineCounter.count(ModeOnlineCounter.Mode.TIERSPACE);
        List<String> lore = withOnlineAndHighlight(mode.lore(), online, highlight);
        return applyHighlight(MenuUtil.item(mode.material(), MenuText.colorize(mode.name()), lore), highlight);
    }

    private static ItemStack buildHardcoreItem(UUID uuid, boolean highlight) {
        HardcoreUnlockStatus unlock = HardcoreUnlockService.evaluate(uuid);
        MainMenuConfig.HardcoreItems items = MainMenuConfig.hardcoreItems();
        int online = ModeOnlineCounter.count(ModeOnlineCounter.Mode.HARDCORE);

        boolean showLocked = unlock.gateEnabled() && !unlock.unlocked();
        ItemStack item;
        if (showLocked) {
            var ph = MainMenuConfig.hardcorePlaceholders(unlock.requiredCoins(), unlock.requiredLevel());
            List<String> lore = withOnlineAndHighlight(
                    MenuText.applyPlaceholders(items.lockedLore(), ph), online, false);
            item = MenuUtil.item(
                    items.lockedMaterial(),
                    MenuText.applyPlaceholders(items.lockedName(), ph),
                    lore
            );
        } else {
            List<String> lore = withOnlineAndHighlight(MenuText.colorizeLore(items.unlockedLore()), online, highlight);
            item = MenuUtil.item(
                    items.unlockedMaterial(),
                    MenuText.colorize(items.unlockedName()),
                    lore
            );
            item = applyHighlight(item, highlight);
        }
        return item;
    }

    private static List<String> withOnlineAndHighlight(List<String> base, int online, boolean highlight) {
        List<String> lore = new ArrayList<>(base);
        lore.add(MenuColors.separator());
        lore.add(MenuColors.neutral("Online ") + MenuColors.success(String.valueOf(online)));
        if (highlight) {
            lore.add(MenuColors.bold(MenuColors.hex("#55FF55", "★ Modo popular")));
        }
        lore.add("");
        lore.add(MenuColors.neutral("Clique para entrar"));
        return lore;
    }

    private static ItemStack applyHighlight(ItemStack item, boolean highlight) {
        if (!highlight) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    /** Compatibilidade com código legado. */
    public static Inventory create() {
        return Bukkit.createInventory(null, SIZE, MainMenuConfig.title());
    }
}

package com.obm.network.smp.hud;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.hud.HudFeature;
import com.obm.network.core.hud.HudPreferences;
import com.obm.network.core.hud.HudPreferencesService;
import com.obm.network.smp.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class HudGui {

    public static final String TITLE = "§6§lHUD SETTINGS";

    public static final int SLOT_SCOREBOARD = 11;
    public static final int SLOT_ACTIONBAR = 13;
    public static final int SLOT_TITLES = 15;
    public static final int SLOT_SOUNDS = 17;

    private static final int SIZE = 27;

    public void open(Player player) {
        HudPreferencesService service = preferences();
        if (service == null) {
            return;
        }
        HudPreferences prefs = service.get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        fillBackground(inv);
        inv.setItem(SLOT_SCOREBOARD, toggleItem(Material.MAP, "Scoreboard", prefs.isEnabled(HudFeature.SCOREBOARD)));
        inv.setItem(SLOT_ACTIONBAR, toggleItem(Material.EXPERIENCE_BOTTLE, "ActionBar", prefs.isEnabled(HudFeature.ACTIONBAR)));
        inv.setItem(SLOT_TITLES, toggleItem(Material.NAME_TAG, "Titles", prefs.isEnabled(HudFeature.TITLES)));
        inv.setItem(SLOT_SOUNDS, toggleItem(Material.NOTE_BLOCK, "Sounds", prefs.isEnabled(HudFeature.SOUNDS)));
        player.openInventory(inv);
    }

    public void refresh(Player player) {
        if (player.getOpenInventory().getTopInventory() == null) {
            return;
        }
        if (!TITLE.equals(player.getOpenInventory().getTitle())) {
            return;
        }
        open(player);
    }

    public boolean isHudTitle(String title) {
        return TITLE.equals(title);
    }

    public HudFeature featureForSlot(int slot) {
        return switch (slot) {
            case SLOT_SCOREBOARD -> HudFeature.SCOREBOARD;
            case SLOT_ACTIONBAR -> HudFeature.ACTIONBAR;
            case SLOT_TITLES -> HudFeature.TITLES;
            case SLOT_SOUNDS -> HudFeature.SOUNDS;
            default -> null;
        };
    }

    private static ItemStack toggleItem(Material material, String label, boolean enabled) {
        String status = enabled ? "§aAtivo" : "§cDesativado";
        return GuiItems.named(material, "§e§l" + label, "§7Clica para alternar", "", status);
    }

    private static void fillBackground(Inventory inv) {
        ItemStack filler = GuiItems.named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }

    private static HudPreferencesService preferences() {
        OBMCorePlugin core = OBMCorePlugin.get();
        return core == null ? null : core.getHudPreferencesService();
    }
}

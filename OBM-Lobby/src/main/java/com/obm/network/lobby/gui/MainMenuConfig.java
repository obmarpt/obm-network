package com.obm.network.lobby.gui;

import com.obm.network.lobby.OBMLobbyPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lê {@code main-menu.*} do config.yml do OBM-Lobby.
 */
public final class MainMenuConfig {

    private static final String ROOT = "main-menu.";

    private MainMenuConfig() {
    }

    private static FileConfiguration cfg() {
        return OBMLobbyPlugin.get().getConfig();
    }

    public static String title() {
        return MenuText.colorize(cfg().getString(ROOT + "title", "&8✦ &7Modos"));
    }

    public static Material fillerMaterial() {
        return parseMaterial(cfg().getString(ROOT + "filler.material", "GRAY_STAINED_GLASS_PANE"),
                Material.GRAY_STAINED_GLASS_PANE);
    }

    public static String fillerName() {
        String name = cfg().getString(ROOT + "filler.name", " ");
        return name == null || name.isEmpty() ? " " : MenuText.colorize(name);
    }

    public static int slotSmp() {
        return cfg().getInt(ROOT + "layout.smp", 11);
    }

    public static int slotHardcore() {
        return cfg().getInt(ROOT + "layout.hardcore", 13);
    }

    public static int slotTierSpace() {
        return cfg().getInt(ROOT + "layout.tierspace", 15);
    }

    public static boolean useTitleFeedback() {
        return cfg().getBoolean(ROOT + "feedback.use-title", true);
    }

    public static int titleFadeIn() {
        return cfg().getInt(ROOT + "feedback.title-fade-in", 5);
    }

    public static int titleStay() {
        return cfg().getInt(ROOT + "feedback.title-stay", 20);
    }

    public static int titleFadeOut() {
        return cfg().getInt(ROOT + "feedback.title-fade-out", 10);
    }

    public static ModeItem smpItem() {
        return modeItem("smp", Material.GRASS_BLOCK, "&6&l⚔ SMP",
                List.of("&7Survival · Economia · PvP"),
                "&aA entrar em SMP...");
    }

    public static ModeItem tierSpaceItem() {
        return modeItem("tierspace", Material.DIAMOND_SWORD, "&b&l🏆 TierSpace",
                List.of("&7Ranked 1v1 · Elo · Seasons"),
                "&bA entrar em TierSpace...");
    }

    public static HardcoreItems hardcoreItems() {
        Material lockedMat = parseMaterial(cfg().getString(ROOT + "modes.hardcore.locked.material", "BARRIER"),
                Material.BARRIER);
        String lockedName = cfg().getString(ROOT + "modes.hardcore.locked.name", "&c&l🔒 Hardcore");
        List<String> lockedLore = cfg().getStringList(ROOT + "modes.hardcore.locked.lore");
        if (lockedLore.isEmpty()) {
            lockedLore = List.of(
                    "&7Desbloqueado em:",
                    "&e{coins} Money",
                    "&eNível {level}"
            );
        }

        Material unlockedMat = parseMaterial(cfg().getString(ROOT + "modes.hardcore.unlocked.material", "NETHERITE_SWORD"),
                Material.NETHERITE_SWORD);
        String unlockedName = cfg().getString(ROOT + "modes.hardcore.unlocked.name", "&c&l☠ Hardcore");
        List<String> unlockedLore = cfg().getStringList(ROOT + "modes.hardcore.unlocked.lore");
        if (unlockedLore.isEmpty()) {
            unlockedLore = List.of("&7Vidas limitadas · alto risco");
        }

        String entering = cfg().getString(ROOT + "modes.hardcore.entering", "&cA entrar em Hardcore...");
        return new HardcoreItems(lockedMat, lockedName, lockedLore, unlockedMat, unlockedName, unlockedLore, entering);
    }

    private static ModeItem modeItem(String key, Material defMat, String defName, List<String> defLore, String defEntering) {
        String path = ROOT + "modes." + key + ".";
        Material mat = parseMaterial(cfg().getString(path + "material"), defMat);
        String name = cfg().getString(path + "name", defName);
        List<String> lore = cfg().getStringList(path + "lore");
        if (lore.isEmpty()) {
            lore = defLore;
        }
        String entering = cfg().getString(path + "entering", defEntering);
        return new ModeItem(mat, name, lore, entering);
    }

    public static Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Material.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            OBMLobbyPlugin.get().getLogger().warning("Material inválido no menu: " + name);
            return fallback;
        }
    }

    public record ModeItem(Material material, String name, List<String> lore, String enteringMessage) {
    }

    public record HardcoreItems(
            Material lockedMaterial,
            String lockedName,
            List<String> lockedLore,
            Material unlockedMaterial,
            String unlockedName,
            List<String> unlockedLore,
            String enteringMessage
    ) {
    }

    public static Map<String, String> hardcorePlaceholders(int requiredCoins, int requiredLevel) {
        String coinsShort = formatCoinsShort(requiredCoins);
        return Map.of(
                "coins", coinsShort,
                "level", String.valueOf(requiredLevel)
        );
    }

    private static String formatCoinsShort(int amount) {
        if (amount >= 1_000_000) {
            return (amount / 1_000_000) + "M";
        }
        if (amount >= 1_000) {
            return (amount / 1_000) + "K";
        }
        return String.format(Locale.US, "%,d", amount);
    }
}

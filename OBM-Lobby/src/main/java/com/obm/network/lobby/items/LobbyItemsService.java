package com.obm.network.lobby.items;

import com.obm.network.lobby.OBMLobbyPlugin;
import com.obm.network.lobby.gui.MenuText;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Locale;

/**
 * Hotbar fixa do lobby: Navegador · Battle Pass · Perfil · Voltar.
 */
public final class LobbyItemsService {

    public static final String TAG = "§r§8OBM-Lobby";

    private static String typeTag(LobbyItem item) {
        return TAG + ":" + item.configKey();
    }

    public enum LobbyItem {
        NAVIGATOR("navigator", Material.COMPASS),
        BATTLE_PASS("battlepass", Material.NETHER_STAR),
        PROFILE("profile", Material.PLAYER_HEAD),
        BACK("back", Material.RED_BED);

        private final String configKey;
        private final Material defaultMaterial;

        LobbyItem(String configKey, Material defaultMaterial) {
            this.configKey = configKey;
            this.defaultMaterial = defaultMaterial;
        }

        public String configKey() {
            return configKey;
        }

        public Material defaultMaterial() {
            return defaultMaterial;
        }
    }

    private LobbyItemsService() {
    }

    public static void equip(Player player) {
        if (player == null) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(null);
        inv.setExtraContents(null);

        inv.setItem(slot(LobbyItem.NAVIGATOR), build(player, LobbyItem.NAVIGATOR));
        inv.setItem(slot(LobbyItem.BATTLE_PASS), build(player, LobbyItem.BATTLE_PASS));
        inv.setItem(slot(LobbyItem.PROFILE), build(player, LobbyItem.PROFILE));
        inv.setItem(slot(LobbyItem.BACK), build(player, LobbyItem.BACK));
    }

    public static boolean isLobbyItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return false;
        }
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            return false;
        }
        return lore.get(lore.size() - 1).startsWith(TAG);
    }

    public static LobbyItem typeOf(ItemStack item) {
        if (!isLobbyItem(item)) {
            return null;
        }
        String last = item.getItemMeta().getLore().get(item.getItemMeta().getLore().size() - 1);
        for (LobbyItem li : LobbyItem.values()) {
            if (last.equals(typeTag(li))) {
                return li;
            }
        }
        return null;
    }

    public static int slot(LobbyItem item) {
        String path = "lobby-items." + item.configKey() + ".slot";
        int def = switch (item) {
            case NAVIGATOR -> 0;
            case BATTLE_PASS -> 1;
            case PROFILE -> 2;
            case BACK -> 8;
        };
        return OBMLobbyPlugin.get().getConfig().getInt(path, def);
    }

    private static ItemStack build(Player player, LobbyItem item) {
        String root = "lobby-items." + item.configKey() + ".";
        Material mat = parseMaterial(
                OBMLobbyPlugin.get().getConfig().getString(root + "material"),
                item.defaultMaterial());
        String name = MenuText.colorize(
                OBMLobbyPlugin.get().getConfig().getString(root + "name", defaultName(item)));
        List<String> lore = MenuText.colorizeLore(
                OBMLobbyPlugin.get().getConfig().getStringList(root + "lore"));
        if (lore.isEmpty()) {
            lore = MenuText.colorizeLore(defaultLore(item));
        }
        lore = new java.util.ArrayList<>(lore);
        lore.add(typeTag(item));

        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            if (item == LobbyItem.PROFILE && meta instanceof SkullMeta skull) {
                skull.setOwningPlayer(player);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String defaultName(LobbyItem item) {
        return switch (item) {
            case NAVIGATOR -> "&e&lNavegador";
            case BATTLE_PASS -> "&6&lBattle Pass";
            case PROFILE -> "&b&lPerfil";
            case BACK -> "&c&lVoltar";
        };
    }

    private static List<String> defaultLore(LobbyItem item) {
        return switch (item) {
            case NAVIGATOR -> List.of("&7Escolhe um modo de jogo", "&eClique para abrir");
            case BATTLE_PASS -> List.of("&7Recompensas e quests", "&eClique para ver");
            case PROFILE -> List.of("&7As tuas estatísticas", "&eClique para abrir");
            case BACK -> List.of("&7Regressar ao spawn", "&eClique para voltar");
        };
    }

    private static Material resolveMaterial(LobbyItem item) {
        String path = "lobby-items." + item.configKey() + ".material";
        return parseMaterial(
                OBMLobbyPlugin.get().getConfig().getString(path),
                item.defaultMaterial());
    }

    private static Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Material.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}

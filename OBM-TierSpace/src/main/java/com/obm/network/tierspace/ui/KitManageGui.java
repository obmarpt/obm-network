package com.obm.network.tierspace.ui;

import com.obm.network.tierspace.kit.KitPreference;
import com.obm.network.tierspace.kit.PlayerKitService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public final class KitManageGui {

    public static final String TITLE = TierMenuColors.bold(TierMenuColors.accent("KIT PVP"));

    public static final int SLOT_DEFAULT = 11;
    public static final int SLOT_CUSTOM = 13;
    public static final int SLOT_RANDOM = 15;
    public static final int SLOT_SAVE = 22;
    public static final int SLOT_BACK = 49;

    private final PlayerKitService playerKitService;

    public KitManageGui(PlayerKitService playerKitService) {
        this.playerKitService = playerKitService;
    }

    public Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        UUID uuid = player.getUniqueId();
        KitPreference pref = playerKitService.getPreference(uuid);

        inv.setItem(SLOT_DEFAULT, item(Material.CHEST, "§fKit do modo",
                pref == KitPreference.DEFAULT,
                List.of(TierMenuColors.separator(), "§7Usa o kit configurado do modo", "", "§eClique para ativar")));
        inv.setItem(SLOT_CUSTOM, item(Material.ENDER_CHEST, "§aKit personalizado",
                pref == KitPreference.CUSTOM,
                List.of(TierMenuColors.separator(), "§7Guarda o teu inventário atual", "",
                        playerKitService.hasCustomKit(uuid) ? "§aKit guardado" : "§cSem kit guardado")));
        inv.setItem(SLOT_RANDOM, item(Material.NETHER_STAR, "§dKit aleatório",
                pref == KitPreference.RANDOM,
                List.of(TierMenuColors.separator(), "§7Kit random em cada match", "", "§eClique para ativar")));
        inv.setItem(SLOT_SAVE, item(Material.LIME_CONCRETE, "§a§lGuardar kit atual",
                false, List.of("§7Salva armadura + inventário", TierMenuColors.primary("▶ Clique"))));
        inv.setItem(SLOT_BACK, item(Material.ARROW, "§7Voltar", false, List.of("§7Queue Duels")));
        return inv;
    }

    private static org.bukkit.inventory.ItemStack item(Material material, String name, boolean selected,
                                                       List<String> lore) {
        org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((selected ? "§a§l" : "§f") + name);
            java.util.ArrayList<String> lines = new java.util.ArrayList<>(lore);
            if (selected) {
                lines.add("");
                lines.add("§a§l✔ ATIVO");
            }
            meta.setLore(lines);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}

package com.obm.network.lobby.gui;

import com.obm.network.core.integration.EconomyBridge;
import com.obm.network.core.integration.SMPBridge;
import com.obm.network.core.integration.TierSpaceBridge;
import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.hardcore.HardcoreUnlockService;
import com.obm.network.core.hardcore.HardcoreUnlockStatus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * Main Menu — 27 slots (3×9)
 * <p>
 * Slot 4  → Stats gerais
 * Slot 11 → Rush SMP
 * Slot 13 → TierSpace
 * Slot 15 → Hardcore
 * Slot 22 → Perfil
 */
public class MainMenu {

    public static final int SLOT_STATS = 4;
    public static final int SLOT_RUSH = 11;
    public static final int SLOT_TIERSPACE = 13;
    public static final int SLOT_HARDCORE = 15;
    public static final int SLOT_PROFILE = 22;

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MenuTitles.MAIN);
        MenuUtil.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        UUID uuid = player.getUniqueId();
        SMPBridge.ProgressionSnapshot progression = SMPBridge.getProgression(uuid);
        int coins = EconomyBridge.getBalance(uuid);
        int tierRating = TierSpaceBridge.getRating(uuid, "sword");
        HardcoreUnlockStatus hardcoreUnlock = HardcoreUnlockService.evaluate(uuid);

        inv.setItem(SLOT_STATS, MenuUtil.item(
                Material.NETHER_STAR,
                MenuColors.bold(MenuColors.white("✦ " + player.getName())),
                MenuColors.separator(),
                MenuColors.neutral("Coins ") + MenuColors.rush(String.valueOf(coins)),
                MenuColors.neutral("Rank ") + MenuColors.rush(progression.rankName()),
                MenuColors.neutral("Level ") + MenuColors.white(String.valueOf(progression.level())),
                MenuColors.neutral("TierSpace ") + MenuColors.tier(String.valueOf(tierRating)),
                "",
                MenuColors.neutral("Escolhe um modo abaixo")
        ));

        inv.setItem(SLOT_RUSH, MenuUtil.item(
                Material.GOLDEN_SWORD,
                MenuColors.bold(MenuColors.rush("⚔ RUSH SMP")),
                MenuColors.separator(),
                MenuColors.neutral("Farm → Vender → Gear → PvP"),
                "",
                MenuColors.neutral("Mundo ") + MenuColors.white("world"),
                MenuColors.neutral("Economia ") + MenuColors.success("Activa"),
                "",
                MenuColors.action()
        ));

        inv.setItem(SLOT_TIERSPACE, MenuUtil.item(
                Material.DIAMOND_SWORD,
                MenuColors.bold(MenuColors.tier("🏆 TIERSPACE")),
                MenuColors.separator(),
                MenuColors.neutral("PvP competitivo 1v1"),
                "",
                MenuColors.neutral("Rating ") + MenuColors.tier(String.valueOf(tierRating)),
                MenuColors.neutral("Ranking ") + MenuColors.tierAccent("Por modo"),
                "",
                MenuColors.actionTier()
        ));

        inv.setItem(SLOT_HARDCORE, buildHardcoreItem(uuid, hardcoreUnlock));

        ItemStack profileHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta profileMeta = (SkullMeta) profileHead.getItemMeta();
        if (profileMeta != null) {
            profileMeta.setOwningPlayer(player);
            profileMeta.setDisplayName(MenuColors.bold(MenuColors.hex("#FFD700", "👤 PERFIL")));
            profileMeta.setLore(java.util.List.of(
                    MenuColors.separator(),
                    MenuColors.neutral("Estatísticas globais"),
                    MenuColors.neutral("Rush · Hardcore · TierSpace"),
                    "",
                    MenuColors.action()
            ));
            profileHead.setItemMeta(profileMeta);
        }
        inv.setItem(SLOT_PROFILE, profileHead);

        return inv;
    }

    public static Inventory create() {
        return Bukkit.createInventory(null, 27, MenuTitles.MAIN);
    }

    private static org.bukkit.inventory.ItemStack buildHardcoreItem(UUID uuid, HardcoreUnlockStatus unlock) {
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(HardcoreUnlockLore.SEP);

        if (unlock.gateEnabled()) {
            if (unlock.unlocked()) {
                lore.add("§a✅ Desbloqueado");
                lore.add("§7§e§l👉 Clica para jogar");
            } else {
                lore.addAll(HardcoreUnlockLore.mainMenuLockedLore(unlock));
            }
            lore.add("");
        } else {
            lore.add("§7Sobrevive. Evolui. Morre.");
            lore.add("");
        }

        lore.add("§7Vidas: §c" + OBMCorePlugin.get().getDataStore().getInt(uuid, "lives_uhc"));
        lore.add("");
        lore.add(unlock.canEnter() ? "§e§l👉 Abrir menu" : "§7Clica para ver detalhes");

        Material icon = unlock.gateEnabled() && !unlock.unlocked()
                ? Material.BARRIER
                : Material.TOTEM_OF_UNDYING;

        return MenuUtil.item(icon, "§c§l💀 Hardcore", lore);
    }
}

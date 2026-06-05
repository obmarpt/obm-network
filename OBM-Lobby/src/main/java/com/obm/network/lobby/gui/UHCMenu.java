package com.obm.network.lobby.gui;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.hardcore.HardcoreUnlockService;
import com.obm.network.core.hardcore.HardcoreUnlockStatus;
import com.obm.network.core.storage.DataStore;
import com.obm.network.lobby.hardcore.HardcoreUnlockCelebration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Hardcore Menu — 27 slots (estrutura fixa)
 * <p>
 * Slot 4  → 💀 Hardcore (estado + requisitos)
 * Slot 11 → Estatísticas UHC
 * Slot 13 → Info modo / reforço locked
 * Slot 18 → Voltar
 * Slot 22 → Jogar
 */
public class UHCMenu {

    public static final int SLOT_HEADER = 4;
    public static final int SLOT_STATS = 11;
    public static final int SLOT_UNLOCK = 13;
    public static final int SLOT_BACK = 18;
    public static final int SLOT_PLAY = 22;

    /** @deprecated use {@link #SLOT_HEADER} */
    public static final int SLOT_STATUS = SLOT_HEADER;

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MenuTitles.HARDCORE);
        MenuUtil.fillAll(inv, Material.RED_STAINED_GLASS_PANE);

        UUID uuid = player.getUniqueId();
        DataStore ds = OBMCorePlugin.get().getDataStore();
        HardcoreUnlockStatus unlock = HardcoreUnlockService.evaluate(uuid);

        int lives = com.obm.network.core.integration.HardcoreStatsBridge.getLives(uuid);
        int kills = com.obm.network.core.integration.HardcoreStatsBridge.getKills(uuid);
        int deaths = com.obm.network.core.integration.HardcoreStatsBridge.getDeaths(uuid);
        int mobs = com.obm.network.core.integration.HardcoreStatsBridge.getMobs(uuid);
        int playtime = com.obm.network.core.integration.HardcoreStatsBridge.getPlaytime(uuid);

        inv.setItem(SLOT_HEADER, buildHeaderItem(unlock, lives));
        inv.setItem(SLOT_STATS, buildStatsItem(kills, deaths, mobs, playtime));
        inv.setItem(SLOT_UNLOCK, buildSidePanel(unlock));
        inv.setItem(SLOT_PLAY, buildPlayItem(unlock, lives));
        inv.setItem(SLOT_BACK, MenuUtil.item(
                Material.ARROW,
                "§7← Voltar",
                "§8Menu principal"
        ));

        HardcoreUnlockCelebration.tryCelebrate(player);

        return inv;
    }

    private static ItemStack buildHeaderItem(HardcoreUnlockStatus unlock, int lives) {
        if (!unlock.gateEnabled()) {
            return MenuUtil.item(
                    Material.TOTEM_OF_UNDYING,
                    "§c§l💀 Hardcore",
                    HardcoreUnlockLore.SEP,
                    "§7Modo permanente",
                    "§7Vidas: §c" + lives
            );
        }

        List<String> lore = unlock.unlocked()
                ? HardcoreUnlockLore.unlockedHeaderLore(lives)
                : HardcoreUnlockLore.lockedHeaderLore(unlock, lives);

        Material material = unlock.unlocked() ? Material.TOTEM_OF_UNDYING : Material.BARRIER;
        return MenuUtil.item(material, "§c§l💀 Hardcore", lore);
    }

    private static ItemStack buildStatsItem(int kills, int deaths, int mobs, int playtime) {
        return MenuUtil.item(
                Material.IRON_SWORD,
                "§c§l⚔ Estatísticas",
                HardcoreUnlockLore.SEP,
                "§7Kills: §f" + kills,
                "§7Deaths: §f" + deaths,
                "§7K/D: §c" + MenuUtil.formatKd(kills, deaths),
                "§7Mobs: §f" + mobs,
                "§7Tempo: §f" + MenuUtil.formatTime(playtime)
        );
    }

    private static ItemStack buildSidePanel(HardcoreUnlockStatus unlock) {
        if (!unlock.gateEnabled()) {
            return MenuUtil.item(
                    Material.NETHERITE_CHESTPLATE,
                    "§7§l🌍 Mundo",
                    HardcoreUnlockLore.SEP,
                    "§7Mundo persistente",
                    "§7Progressão lenta",
                    "§7Inventário partilhado"
            );
        }

        if (unlock.unlocked()) {
            return MenuUtil.item(
                    Material.LIME_STAINED_GLASS_PANE,
                    "§a§l✓ Desbloqueado",
                    HardcoreUnlockLore.SEP,
                    "§7Requisitos cumpridos",
                    "§c⚠ Morte = perder vida"
            );
        }

        return MenuUtil.item(
                Material.GRAY_STAINED_GLASS_PANE,
                "§7§lℹ Como desbloquear",
                HardcoreUnlockLore.SEP,
                "§7Joga Rush: farm coins",
                "§7e sobe de level"
        );
    }

    private static ItemStack buildPlayItem(HardcoreUnlockStatus unlock, int lives) {
        if (!unlock.canEnter()) {
            return MenuUtil.item(
                    Material.BARRIER,
                    "§c§l🔒 Bloqueado",
                    HardcoreUnlockLore.SEP,
                    "§7Vê requisitos em §c💀 Hardcore"
            );
        }

        if (lives <= 0) {
            return MenuUtil.item(
                    Material.BARRIER,
                    "§7§lSEM VIDAS",
                    HardcoreUnlockLore.SEP,
                    "§cSem vidas disponíveis",
                    "§7Aguarda revive"
            );
        }

        return MenuUtil.item(
                Material.FLINT_AND_STEEL,
                "§a§l✅ Desbloqueado",
                HardcoreUnlockLore.SEP,
                "§e§l👉 Clica para jogar",
                "§c⚠ Morrer = perder vida"
        );
    }
}

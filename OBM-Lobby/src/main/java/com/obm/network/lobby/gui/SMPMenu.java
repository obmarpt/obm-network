package com.obm.network.lobby.gui;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.integration.EconomyBridge;
import com.obm.network.core.integration.SMPBridge;
import com.obm.network.core.storage.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * Rush Menu — 27 slots
 * Slot 4 economia · 11 combate · 13 progresso · 15 loja · 22 leilão · 26 jogar · 18 voltar
 */
public class SMPMenu {

    public static final int SLOT_ECONOMY = 4;
    public static final int SLOT_COMBAT = 11;
    public static final int SLOT_PROGRESS = 13;
    public static final int SLOT_SHOP = 15;
    public static final int SLOT_AUCTION = 22;
    public static final int SLOT_PLAY = 26;
    public static final int SLOT_BACK = 18;

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MenuTitles.RUSH);
        MenuUtil.fillAll(inv, Material.ORANGE_STAINED_GLASS_PANE);

        UUID uuid = player.getUniqueId();
        DataStore ds = OBMCorePlugin.get().getDataStore();
        SMPBridge.ProgressionSnapshot progression = SMPBridge.getProgression(uuid);

        int time = ds.getInt(uuid, "playtime_smp");
        int blocks = ds.getInt(uuid, com.obm.network.core.storage.PlayerStatsKeys.BLOCKS_BROKEN_SMP);
        int kills = ds.getInt(uuid, "kills_smp");
        int deaths = ds.getInt(uuid, "deaths_smp");
        int money = EconomyBridge.getBalance(uuid);

        inv.setItem(SLOT_ECONOMY, MenuUtil.item(
                Material.EMERALD,
                MenuColors.bold(MenuColors.rush("💰 ECONOMIA")),
                MenuColors.separator(),
                MenuColors.neutral("Coins ") + MenuColors.rush(String.valueOf(money)),
                MenuColors.neutral("Ganhos ") + MenuColors.white(String.valueOf(EconomyBridge.getTotalEarned(uuid))),
                MenuColors.neutral("Rank ") + MenuColors.rush(progression.rankName())
        ));

        inv.setItem(SLOT_COMBAT, MenuUtil.item(
                Material.IRON_SWORD,
                MenuColors.bold(MenuColors.hardcore("⚔ COMBATE")),
                MenuColors.separator(),
                MenuColors.neutral("Kills ") + MenuColors.white(String.valueOf(kills)),
                MenuColors.neutral("Deaths ") + MenuColors.white(String.valueOf(deaths)),
                MenuColors.neutral("K/D ") + MenuColors.rush(MenuUtil.formatKd(kills, deaths))
        ));

        inv.setItem(SLOT_PROGRESS, MenuUtil.item(
                Material.EXPERIENCE_BOTTLE,
                MenuColors.bold(MenuColors.rush("📈 PROGRESSO")),
                MenuColors.separator(),
                MenuColors.neutral("Level ") + MenuColors.white(String.valueOf(progression.level())),
                MenuColors.neutral("XP ") + MenuColors.white(progression.xp() + "/" + progression.xpRequired()),
                MenuColors.neutral("Blocos ") + MenuColors.white(String.valueOf(blocks)),
                MenuColors.neutral("Tempo ") + MenuColors.white(MenuUtil.formatTime(time))
        ));

        inv.setItem(SLOT_SHOP, MenuUtil.item(
                Material.GOLDEN_APPLE,
                MenuColors.bold(MenuColors.rush("🛒 LOJA")),
                MenuColors.separator(),
                MenuColors.neutral("Compra itens e gear"),
                MenuColors.neutral("Desconto ") + MenuColors.rush((int) (progression.shopDiscount() * 100) + "%"),
                "",
                MenuColors.action()
        ));

        inv.setItem(SLOT_AUCTION, MenuUtil.item(
                Material.CHEST,
                MenuColors.bold(MenuColors.hex("#CC88FF", "📦 LEILÃO")),
                MenuColors.separator(),
                MenuColors.neutral("Mercado entre jogadores"),
                "",
                MenuColors.action()
        ));

        inv.setItem(SLOT_PLAY, MenuUtil.item(
                Material.COMPASS,
                MenuColors.bold(MenuColors.rush("▶ JOGAR RUSH")),
                MenuColors.separator(),
                MenuColors.neutral("Teleporta para o mundo"),
                MenuColors.neutral("Inventário guardado automaticamente"),
                "",
                MenuColors.bold(MenuColors.rush("CLIQUE PARA ENTRAR"))
        ));

        inv.setItem(SLOT_BACK, MenuUtil.item(
                Material.ARROW,
                MenuColors.neutral("← Voltar"),
                MenuColors.neutral("Menu principal")
        ));

        return inv;
    }
}

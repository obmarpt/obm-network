package com.obm.network.lobby.gui;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.economy.CurrencyLabels;
import com.obm.network.core.integration.EconomyBridge;
import com.obm.network.core.integration.SMPBridge;
import com.obm.network.core.integration.TierSpaceBridge;
import com.obm.network.core.progression.ProgressionLevelService;
import com.obm.network.core.storage.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * Perfil — 27 slots
 */
public class ProfileMenu {

    public static final int SLOT_HEAD = 4;
    public static final int SLOT_RUSH = 11;
    public static final int SLOT_HARDCORE = 13;
    public static final int SLOT_TIERSPACE = 15;
    public static final int SLOT_PROGRESS = 22;
    public static final int SLOT_BACK = 18;

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MenuTitles.PROFILE);
        MenuUtil.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        UUID uuid = player.getUniqueId();
        DataStore ds = OBMCorePlugin.get().getDataStore();
        SMPBridge.ProgressionSnapshot progression = SMPBridge.getProgression(uuid);
        ProgressionLevelService levels = OBMCorePlugin.get().getProgressionLevelService();
        if (levels != null) {
            levels.ensureInitialized(uuid);
        }
        int hcLevel = levels != null ? levels.getHcLevel(uuid) : 1;
        int globalLevel = levels != null ? levels.getGlobalLevel(uuid) : 1;

        int rushKills = ds.getInt(uuid, "kills_smp");
        int rushDeaths = ds.getInt(uuid, "deaths_smp");
        int hcKills = ds.getInt(uuid, "kills_uhc");
        int hcDeaths = ds.getInt(uuid, "deaths_uhc");
        int hcLives = ds.getInt(uuid, "lives_uhc");
        int playtime = ds.getInt(uuid, "playtime_smp") + ds.getInt(uuid, "playtime_uhc");
        int emeralds = OBMCorePlugin.get().getGlobalEconomy() != null
                ? OBMCorePlugin.get().getGlobalEconomy().getBalance(uuid)
                : 0;

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(MenuColors.bold(MenuColors.hex("#FFD700", player.getName())));
            skullMeta.setLore(java.util.List.of(
                    MenuColors.separator(),
                    MenuColors.neutral("Tempo ") + MenuColors.white(MenuUtil.formatTime(playtime)),
                    MenuColors.neutral("Ping ") + MenuColors.white(player.getPing() + "ms"),
                    "",
                    MenuColors.rush("SMP Level ") + MenuColors.white(String.valueOf(progression.level())),
                    MenuColors.hardcore("HC Level ") + MenuColors.white(String.valueOf(hcLevel)),
                    MenuColors.tier("Global Level ") + MenuColors.white(String.valueOf(globalLevel)),
                    "",
                    MenuColors.rush(CurrencyLabels.SMP_MONEY + " ") + MenuColors.white(
                            CurrencyLabels.formatAmount(EconomyBridge.getBalance(uuid))),
                    MenuColors.tier(CurrencyLabels.GLOBAL_EMERALDS + " ") + MenuColors.white(
                            CurrencyLabels.formatAmount(emeralds))
            ));
            head.setItemMeta(skullMeta);
        }
        inv.setItem(SLOT_HEAD, head);

        inv.setItem(SLOT_RUSH, MenuUtil.item(
                Material.GOLDEN_SWORD,
                MenuColors.bold(MenuColors.rush("⚔ RUSH")),
                MenuColors.separator(),
                MenuColors.neutral("Kills ") + MenuColors.white(String.valueOf(rushKills)),
                MenuColors.neutral("Deaths ") + MenuColors.white(String.valueOf(rushDeaths)),
                MenuColors.neutral("K/D ") + MenuColors.rush(MenuUtil.formatKd(rushKills, rushDeaths)),
                MenuColors.neutral("Rank ") + MenuColors.rush(progression.rankName())
        ));

        inv.setItem(SLOT_HARDCORE, MenuUtil.item(
                Material.TOTEM_OF_UNDYING,
                MenuColors.bold(MenuColors.hardcore("💀 HARDCORE")),
                MenuColors.separator(),
                MenuColors.neutral("Kills ") + MenuColors.white(String.valueOf(hcKills)),
                MenuColors.neutral("Deaths ") + MenuColors.white(String.valueOf(hcDeaths)),
                MenuColors.neutral("K/D ") + MenuColors.hardcore(MenuUtil.formatKd(hcKills, hcDeaths)),
                MenuColors.neutral("Vidas ") + MenuColors.hardcore(String.valueOf(hcLives))
        ));

        inv.setItem(SLOT_TIERSPACE, MenuUtil.item(
                Material.DIAMOND_SWORD,
                MenuColors.bold(MenuColors.tier("🏆 TIERSPACE")),
                MenuColors.separator(),
                MenuColors.neutral("Rating ") + MenuColors.tier(String.valueOf(TierSpaceBridge.getRating(uuid, "sword"))),
                MenuColors.neutral("Rank ") + TierSpaceBridge.getRankDisplay(uuid, "sword"),
                MenuColors.neutral("W/L ") + MenuColors.success(String.valueOf(TierSpaceBridge.getWins(uuid, "sword")))
                        + MenuColors.neutral("/") + MenuColors.error(String.valueOf(TierSpaceBridge.getLosses(uuid, "sword"))),
                MenuColors.neutral("Streak ") + MenuColors.tier(String.valueOf(TierSpaceBridge.getStreak(uuid, "sword")))
        ));

        inv.setItem(SLOT_PROGRESS, MenuUtil.item(
                Material.NETHER_STAR,
                MenuColors.bold(MenuColors.hex("#55FFFF", "✦ PROGRESSO")),
                MenuColors.separator(),
                MenuColors.rush("SMP Level ") + MenuColors.white(String.valueOf(progression.level()))
                        + " §8(" + progression.xp() + "/" + progression.xpRequired() + " XP)",
                MenuColors.hardcore("HC Level ") + MenuColors.white(String.valueOf(hcLevel)),
                MenuColors.tier("Global Level ") + MenuColors.white(String.valueOf(globalLevel)),
                MenuColors.separator(),
                MenuColors.neutral("Rank Rush ") + MenuColors.rush(progression.rankName()),
                progression.maxRank()
                        ? MenuColors.success("Rank máximo!")
                        : MenuColors.neutral("Próximo ") + MenuColors.rush(progression.nextRankName())
                        + MenuColors.neutral(" · ") + MenuColors.rush(progression.nextRankCost() + " Money")
        ));

        inv.setItem(SLOT_BACK, MenuUtil.item(
                Material.ARROW,
                MenuColors.neutral("← Voltar"),
                MenuColors.neutral("Menu principal")
        ));

        return inv;
    }
}

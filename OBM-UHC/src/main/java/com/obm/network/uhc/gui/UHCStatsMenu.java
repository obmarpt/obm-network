package com.obm.network.uhc.gui;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.progression.ProgressionLevelService;
import com.obm.network.core.storage.DataStore;
import com.obm.network.uhc.util.UHCUtils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Painel de estatísticas Hardcore — 27 slots.
 * <p>
 * 10 Kills · 11 KDR · 12 Wins · 13 Deaths · 14 Playtime · 16 Back · 22 Summary
 */
public final class UHCStatsMenu {

    public static final String TITLE = "§c§l💀 HARDCORE STATS";

    public static final int SLOT_KILLS = 10;
    public static final int SLOT_KDR = 11;
    public static final int SLOT_WINS = 12;
    public static final int SLOT_DEATHS = 13;
    public static final int SLOT_PLAYTIME = 14;
    public static final int SLOT_BACK = 16;
    public static final int SLOT_SUMMARY = 22;

    private static final String KILLS_KEY = "kills_uhc";
    private static final String DEATHS_KEY = "deaths_uhc";
    private static final String WINS_KEY = "wins_uhc";
    private static final String MOBS_KEY = "mobs_uhc";
    private static final String PLAYTIME_KEY = "playtime_uhc";
    private static final String TIME_ALIVE_KEY = "time_alive_uhc";

    private UHCStatsMenu() {
    }

    public static void open(Player player) {
        player.openInventory(create(player));
    }

    public static Inventory create(Player player) {
        UUID uuid = player.getUniqueId();
        DataStore ds = OBMCorePlugin.get().getDataStore();

        int lives = UHCUtils.getLives(player);
        int kills = ds.getInt(uuid, KILLS_KEY);
        int deaths = ds.getInt(uuid, DEATHS_KEY);
        int wins = ds.getInt(uuid, WINS_KEY);
        int mobs = ds.getInt(uuid, MOBS_KEY);
        int playtimeSec = ds.getInt(uuid, PLAYTIME_KEY);
        int timeAliveSec = ds.getInt(uuid, TIME_ALIVE_KEY);
        String kdr = formatKd(kills, deaths);

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        fillAll(inv, Material.RED_STAINED_GLASS_PANE);

        inv.setItem(SLOT_KILLS, statItem(
                Material.IRON_SWORD,
                "§c§l⚔ KILLS",
                "§8━━━━━━━━━━━━━━━━",
                "§7Total de eliminações",
                "",
                "§f§l" + kills,
                "",
                "§8Jogadores abatidos no Hardcore"
        ));

        inv.setItem(SLOT_KDR, statItem(
                Material.GOLDEN_APPLE,
                "§c§l📊 K/D RATIO",
                "§8━━━━━━━━━━━━━━━━",
                "§7Kills §f" + kills + " §8· §7Deaths §f" + deaths,
                "",
                "§f§l" + kdr,
                "",
                "§8Rácio kills / mortes"
        ));

        inv.setItem(SLOT_WINS, statItem(
                Material.GOLDEN_HELMET,
                "§c§l🏆 WINS",
                "§8━━━━━━━━━━━━━━━━",
                "§7Vitórias registadas",
                "",
                "§f§l" + wins,
                "",
                "§8Partidas ganhas no modo"
        ));

        inv.setItem(SLOT_DEATHS, statItem(
                Material.SKELETON_SKULL,
                "§c§l☠ DEATHS",
                "§8━━━━━━━━━━━━━━━━",
                "§7Total de mortes",
                "",
                "§f§l" + deaths,
                "",
                "§8Mortes no mundo Hardcore"
        ));

        inv.setItem(SLOT_PLAYTIME, statItem(
                Material.CLOCK,
                "§c§l⏳ PLAYTIME",
                "§8━━━━━━━━━━━━━━━━",
                "§7Tempo no modo",
                "",
                "§f§l" + formatTime(playtimeSec),
                "",
                "§8Tempo vivo (sessão): §f" + formatTime(timeAliveSec)
        ));

        inv.setItem(SLOT_BACK, statItem(
                Material.ARROW,
                "§7§l← VOLTAR",
                "§8━━━━━━━━━━━━━━━━",
                "§7Fecha este painel",
                "",
                "§e§l▶ Clique para fechar"
        ));

        ProgressionLevelService levels = OBMCorePlugin.get().getProgressionLevelService();
        if (levels != null) {
            levels.ensureInitialized(uuid);
        }
        int hcLevel = levels != null ? levels.getHcLevel(uuid) : 1;
        int globalLevel = levels != null ? levels.getGlobalLevel(uuid) : 1;
        inv.setItem(SLOT_SUMMARY, buildSummaryHead(player, lives, kills, deaths, wins, mobs, kdr,
                playtimeSec, timeAliveSec, hcLevel, globalLevel));

        return inv;
    }

    private static ItemStack buildSummaryHead(
            Player player,
            int lives,
            int kills,
            int deaths,
            int wins,
            int mobs,
            String kdr,
            int playtimeSec,
            int timeAliveSec,
            int hcLevel,
            int globalLevel
    ) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            return head;
        }

        meta.setOwningPlayer(player);
        meta.setDisplayName("§c§l✦ " + player.getName());

        List<String> lore = new ArrayList<>();
        lore.add("§8━━━━━━━━━━━━━━━━");
        lore.add("§7Resumo Hardcore");
        lore.add("");
        lore.add("§7K/D §f§l" + kdr + " §8(§f" + kills + "§8/§f" + deaths + "§8)");
        lore.add("§cHC Level §f" + hcLevel + " §8· §bGlobal §f" + globalLevel);
        lore.add("§7Wins §f" + wins + " §8· §7Mobs §f" + mobs);
        lore.add("§7Vidas §c§l" + lives);
        lore.add("§7Playtime §f" + formatTime(playtimeSec));
        lore.add("§7Tempo vivo §f" + formatTime(timeAliveSec));
        lore.add("");
        lore.add(protectionLine(player));
        lore.add("");
        lore.add("§8MineSpace · Hardcore");

        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private static String protectionLine(Player player) {
        if (UHCUtils.hasProtection(player)) {
            long remainingMs = UHCUtils.getRemainingProtection(player);
            long totalSec = remainingMs / 1000;
            long h = totalSec / 3600;
            long m = (totalSec % 3600) / 60;
            return "§7Proteção §a" + h + "h " + m + "m";
        }
        return "§7Proteção §cHardcore ativo";
    }

    private static ItemStack statItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void fillAll(Inventory inv, Material pane) {
        ItemStack filler = pane(pane);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§8");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    private static String formatKd(int kills, int deaths) {
        if (deaths == 0) {
            return String.format("%.2f", (double) kills);
        }
        return String.format("%.2f", (double) kills / deaths);
    }
}

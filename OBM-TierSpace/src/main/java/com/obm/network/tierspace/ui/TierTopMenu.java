package com.obm.network.tierspace.ui;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.leaderboard.LeaderboardService;
import com.obm.network.core.tier.TierRankUtil;
import com.obm.network.tierspace.mode.GameModeId;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TierTopMenu {

    public static final String TOP_RATING_TITLE = "§6§lTOP 10 — Rating";
    public static final String TOP_STREAK_TITLE = "§c§lTOP 10 — Best Streak";

    public static Inventory createTopRating(GameModeId mode) {
        Inventory inv = Bukkit.createInventory(null, 54, TOP_RATING_TITLE + " (" + mode.id() + ")");
        LeaderboardService leaderboard = resolveLeaderboard();
        if (leaderboard == null) {
            return inv;
        }
        String statKey = "tierspace_" + mode.id() + "_rating";
        List<UUID> top = leaderboard.getTop(statKey, 10);

        for (int i = 0; i < top.size(); i++) {
            UUID target = top.get(i);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(target);
            int rating = leaderboard.getStat(target, statKey);

            inv.setItem(i, createItem(
                    Material.PLAYER_HEAD,
                    "§e#" + (i + 1) + " §f" + offline.getName(),
                    List.of(
                            "§7Rating: §f" + rating,
                            "§7Rank: §f" + TierRankUtil.fromRating(rating).plainName()
                    )
            ));
        }

        inv.setItem(49, createItem(Material.ARROW, "§bVoltar", List.of("§7TierSpace menu")));
        return inv;
    }

    public static Inventory createTopStreak(GameModeId mode) {
        Inventory inv = Bukkit.createInventory(null, 54, TOP_STREAK_TITLE + " (" + mode.id() + ")");
        LeaderboardService leaderboard = resolveLeaderboard();
        if (leaderboard == null) {
            return inv;
        }
        String statKey = "tierspace_" + mode.id() + "_best_streak";
        List<UUID> top = leaderboard.getTop(statKey, 10);

        for (int i = 0; i < top.size(); i++) {
            UUID target = top.get(i);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(target);
            int streak = leaderboard.getStat(target, statKey);

            inv.setItem(i, createItem(
                    Material.PLAYER_HEAD,
                    "§e#" + (i + 1) + " §f" + offline.getName(),
                    List.of("§7Best streak: §c" + streak)
            ));
        }

        inv.setItem(49, createItem(Material.ARROW, "§bVoltar", List.of("§7TierSpace menu")));
        return inv;
    }

    private static LeaderboardService resolveLeaderboard() {
        OBMCorePlugin core = OBMCorePlugin.get();
        return core == null ? null : core.getLeaderboardService();
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(new ArrayList<>(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}

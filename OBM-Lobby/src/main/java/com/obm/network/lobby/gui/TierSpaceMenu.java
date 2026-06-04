package com.obm.network.lobby.gui;



import com.obm.network.core.integration.TierSpaceBridge;

import com.obm.network.core.leaderboard.LeaderboardService;

import com.obm.network.core.tier.TierRankUtil;

import me.clip.placeholderapi.PlaceholderAPI;

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



public class TierSpaceMenu {



    public static final String TITLE = "§b§lTIER SPACE";

    public static final String TOP_RATING_TITLE = "§6§lTOP 10 — Sword Rating";

    public static final String TOP_STREAK_TITLE = "§c§lTOP 10 — Best Streak";



    private static final String MODE = "sword";



    public static Inventory create(Player player) {

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        UUID uuid = player.getUniqueId();

        boolean inPlacement = TierSpaceBridge.isInPlacement(uuid, MODE);



        List<String> swordLore = new ArrayList<>();

        if (inPlacement) {

            swordLore.add("§7" + TierSpaceBridge.getPlacementLabel(uuid, MODE));

            swordLore.add("§7Rating: §f" + resolvePlaceholder(player, "%obm_tier_rating_" + MODE + "%"));

        } else {

            swordLore.add("§7Rank: " + resolvePlaceholder(player, "%obm_tier_rank_" + MODE + "%"));

            swordLore.add("§7Rating: " + resolvePlaceholder(player, "%obm_tier_rating_" + MODE + "%"));

        }

        swordLore.add("§7Streak: " + resolvePlaceholder(player, "%obm_tier_streak_" + MODE + "%"));

        swordLore.add("");

        swordLore.add("§eClique para entrar na fila!");



        inv.setItem(11, createItem(

                Material.IRON_SWORD,

                "§c⚔ Sword PvP",

                swordLore

        ));



        inv.setItem(13, createItem(

                Material.PAPER,

                "§a📊 Stats",

                List.of("§7Ver estatísticas TierSpace")

        ));



        inv.setItem(15, createItem(

                Material.GOLDEN_HELMET,

                "§6🏆 TOP 10 Rating",

                List.of("§7Melhores jogadores Sword PvP")

        ));



        inv.setItem(17, createItem(

                Material.BLAZE_POWDER,

                "§c🔥 TOP 10 Streak",

                List.of("§7Maiores streaks Sword PvP")

        ));



        inv.setItem(22, createItem(

                Material.ARROW,

                "§bVoltar",

                List.of("§7Menu principal")

        ));



        return inv;

    }



    public static Inventory createTopRating(Player viewer) {

        Inventory inv = Bukkit.createInventory(null, 54, TOP_RATING_TITLE);

        LeaderboardService leaderboard = resolveLeaderboard();
        if (leaderboard == null) {
            inv.setItem(49, createItem(Material.ARROW, "§bVoltar", List.of("§7TierSpace menu")));
            return inv;
        }

        String statKey = "tierspace_sword_rating";

        List<UUID> top = leaderboard.getTop(statKey, 10);



        for (int i = 0; i < top.size(); i++) {

            UUID target = top.get(i);

            OfflinePlayer offline = Bukkit.getOfflinePlayer(target);

            int rating = leaderboard.getStat(target, statKey);

            String rankDisplay = TierRankUtil.fromRating(rating).plainName();



            inv.setItem(i, createItem(

                    Material.PLAYER_HEAD,

                    "§e#" + (i + 1) + " §f" + offline.getName(),

                    List.of(

                            "§7Rating: §f" + rating,

                            "§7Rank: §f" + rankDisplay

                    )

            ));

        }



        inv.setItem(49, createItem(Material.ARROW, "§bVoltar", List.of("§7TierSpace menu")));

        return inv;

    }



    public static Inventory createTopStreak(Player viewer) {

        Inventory inv = Bukkit.createInventory(null, 54, TOP_STREAK_TITLE);

        LeaderboardService leaderboard = resolveLeaderboard();
        if (leaderboard == null) {
            inv.setItem(49, createItem(Material.ARROW, "§bVoltar", List.of("§7TierSpace menu")));
            return inv;
        }

        String statKey = "tierspace_sword_best_streak";

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



    private static String resolvePlaceholder(Player player, String placeholder) {

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {

            return PlaceholderAPI.setPlaceholders(player, placeholder);

        }

        return fallbackTierPlaceholder(player.getUniqueId(), placeholder);

    }



    private static String fallbackTierPlaceholder(UUID uuid, String placeholder) {

        if (placeholder.contains("tier_rank")) {

            return TierSpaceBridge.getRankDisplay(uuid, MODE);

        }

        if (placeholder.contains("tier_rating")) {

            int rating = TierSpaceBridge.getRating(uuid, MODE);

            return rating > 0 ? String.valueOf(rating) : String.valueOf(TierRankUtil.defaultRating());

        }

        if (placeholder.contains("tier_streak")) {

            return String.valueOf(TierSpaceBridge.getStreak(uuid, MODE));

        }

        return "-";

    }



    private static LeaderboardService resolveLeaderboard() {
        var core = com.obm.network.core.OBMCorePlugin.get();
        return core == null ? null : core.getLeaderboardService();
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {

        ItemStack item = new ItemStack(mat);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(name);

            meta.setLore(new ArrayList<>(lore));

            item.setItemMeta(meta);

        }

        return item;

    }

}



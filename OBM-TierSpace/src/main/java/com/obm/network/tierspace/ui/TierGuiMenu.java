package com.obm.network.tierspace.ui;

import com.obm.network.core.integration.TierSpaceBridge;
import com.obm.network.core.tier.TierRankUtil;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.season.TierSeasonManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TierSpace Menu — 54 slots (6×9)
 */
public class TierGuiMenu {

    public static final String TITLE = TierMenuColors.bold(TierMenuColors.primary("TIERSPACE"));

    public static final int SLOT_SEASON = 4;
    public static final int SLOT_TOP_RATING = 47;
    public static final int SLOT_TOP_STREAK = 48;
    public static final int SLOT_SEASON_INFO = 49;
    public static final int SLOT_STATS = 53;

    private final ModeRegistry modeRegistry;
    private final TierSeasonManager seasonManager;

    public TierGuiMenu(ModeRegistry modeRegistry, TierSeasonManager seasonManager) {
        this.modeRegistry = modeRegistry;
        this.seasonManager = seasonManager;
    }

    public Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBackground(inv);
        UUID uuid = player.getUniqueId();

        inv.setItem(SLOT_SEASON, createItem(
                Material.NETHER_STAR,
                TierMenuColors.bold(TierMenuColors.primary("✦ TEMPORADA " + seasonManager.getSeasonNumber())),
                List.of(
                        TierMenuColors.separator(),
                        seasonManager.getSeasonDisplayLine(),
                        "",
                        TierMenuColors.neutral("Escolhe um modo e entra na fila"),
                        TierMenuColors.neutral("Rating · Rank · Streak")
                )
        ));

        for (GameModeId mode : modeRegistry.enabledModes()) {
            inv.setItem(modeRegistry.guiSlot(mode), createModeItem(uuid, mode));
        }

        inv.setItem(SLOT_TOP_RATING, createItem(
                Material.GOLDEN_HELMET,
                TierMenuColors.bold(TierMenuColors.accent("🏆 TOP RATING")),
                List.of(
                        TierMenuColors.neutral("Top 10 — Sword PvP"),
                        "",
                        TierMenuColors.primary("▶ Clique para ver")
                )
        ));

        inv.setItem(SLOT_TOP_STREAK, createItem(
                Material.BLAZE_POWDER,
                TierMenuColors.bold(TierMenuColors.error("🔥 TOP STREAK")),
                List.of(
                        TierMenuColors.neutral("Top 10 — Sword PvP"),
                        "",
                        TierMenuColors.primary("▶ Clique para ver")
                )
        ));

        inv.setItem(SLOT_SEASON_INFO, createItem(
                Material.CLOCK,
                TierMenuColors.bold(TierMenuColors.accent("⏳ SEASON")),
                List.of(
                        seasonManager.getSeasonDisplayLine(),
                        "",
                        TierMenuColors.neutral("Reset suave a cada season")
                )
        ));

        inv.setItem(SLOT_STATS, createItem(
                Material.BOOK,
                TierMenuColors.bold(TierMenuColors.white("📊 MINHAS STATS")),
                List.of(
                        TierMenuColors.neutral("Estatísticas detalhadas"),
                        "",
                        TierMenuColors.primary("▶ Clique para ver")
                )
        ));

        return inv;
    }

    private ItemStack createModeItem(UUID uuid, GameModeId mode) {
        String modeId = mode.id();
        List<String> lore = new ArrayList<>();
        lore.add(TierMenuColors.separator());
        lore.add(TierMenuColors.neutral("Modo ") + TierMenuColors.white(modeRegistry.displayName(mode)));
        lore.add("");

        if (TierSpaceBridge.isInPlacement(uuid, modeId)) {
            lore.add(TierMenuColors.accent(TierSpaceBridge.getPlacementLabel(uuid, modeId)));
            lore.add(TierMenuColors.neutral("Rating ") + TierMenuColors.white(formatRating(uuid, modeId)));
        } else {
            lore.add(TierMenuColors.neutral("Rank ") + TierSpaceBridge.getRankDisplay(uuid, modeId));
            lore.add(TierMenuColors.neutral("Rating ") + TierMenuColors.primary(formatRating(uuid, modeId)));
        }

        lore.add(TierMenuColors.neutral("W/L ") + TierMenuColors.success(String.valueOf(TierSpaceBridge.getWins(uuid, modeId)))
                + TierMenuColors.neutral("/") + TierMenuColors.error(String.valueOf(TierSpaceBridge.getLosses(uuid, modeId))));
        lore.add(TierMenuColors.neutral("Streak ") + TierMenuColors.accent(String.valueOf(TierSpaceBridge.getStreak(uuid, modeId))));
        lore.add("");
        lore.add(TierMenuColors.bold(TierMenuColors.primary("▶ ENTRAR NA QUEUE")));

        return createItem(
                mode.iconMaterial(),
                TierMenuColors.bold(TierMenuColors.white(mode.icon() + " " + modeRegistry.displayName(mode))),
                lore
        );
    }

    private String formatRating(UUID uuid, String modeId) {
        int rating = TierSpaceBridge.getRating(uuid, modeId);
        return rating > 0 ? String.valueOf(rating) : String.valueOf(TierRankUtil.defaultRating());
    }

    private void fillBackground(Inventory inv) {
        ItemStack dark = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack accent = pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        for (int i = 0; i < inv.getSize(); i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) {
                inv.setItem(i, accent);
            } else {
                inv.setItem(i, dark);
            }
        }
    }

    private ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§8");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
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

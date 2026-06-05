package com.obm.network.tierspace.ui;

import com.obm.network.core.integration.TierSpaceBridge;
import com.obm.network.core.tier.CompetitiveTier;
import com.obm.network.core.tier.TierRankUtil;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.mode.TierModeVisual;
import com.obm.network.tierspace.queue.QueueService;
import com.obm.network.tierspace.season.TierSeasonManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Queue Duels — 54 slots (6×9)
 */
public class TierGuiMenu {

    public static final String TITLE = TierMenuColors.bold(TierMenuColors.primary("QUEUE DUELS"));

    public static final int SLOT_SEASON = 4;
    public static final int SLOT_KIT = 40;
    public static final int SLOT_TOP_RATING = 47;
    public static final int SLOT_TOP_STREAK = 48;
    public static final int SLOT_LEAVE = 50;
    public static final int SLOT_STATS = 53;

    private final ModeRegistry modeRegistry;
    private final TierSeasonManager seasonManager;
    private final QueueService queueService;

    public TierGuiMenu(ModeRegistry modeRegistry, TierSeasonManager seasonManager, QueueService queueService) {
        this.modeRegistry = modeRegistry;
        this.seasonManager = seasonManager;
        this.queueService = queueService;
    }

    public Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBackground(inv);
        UUID uuid = player.getUniqueId();
        Optional<GameModeId> queued = queueService.getQueuedMode(uuid);

        inv.setItem(SLOT_SEASON, createItem(
                Material.NETHER_STAR,
                TierMenuColors.bold(TierMenuColors.primary("✦ SEASON " + seasonManager.getSeasonNumber())),
                List.of(
                        TierMenuColors.separator(),
                        seasonManager.getSeasonDisplayLine(),
                        "",
                        TierMenuColors.neutral("Clica num modo para entrar na fila"),
                        TierMenuColors.neutral("Duplo-clique = atualizar fila")
                )
        ));

        for (GameModeId mode : modeRegistry.enabledModes()) {
            inv.setItem(modeRegistry.guiSlot(mode), createModeItem(uuid, mode, queued.orElse(null)));
        }

        inv.setItem(SLOT_KIT, createItem(
                Material.CHEST,
                TierMenuColors.bold(TierMenuColors.accent("🎒 KIT")),
                List.of(TierMenuColors.separator(), TierMenuColors.neutral("Kit próprio / random"), "",
                        TierMenuColors.primary("▶ Gerir kit"))
        ));

        inv.setItem(SLOT_TOP_RATING, createItem(Material.GOLDEN_HELMET,
                TierMenuColors.bold(TierMenuColors.accent("🏆 TOP RATING")),
                List.of(TierMenuColors.neutral("Top 10 — Sword"), "", TierMenuColors.primary("▶ Ver"))));
        inv.setItem(SLOT_TOP_STREAK, createItem(Material.BLAZE_POWDER,
                TierMenuColors.bold(TierMenuColors.error("🔥 TOP STREAK")),
                List.of(TierMenuColors.neutral("Top 10 — Sword"), "", TierMenuColors.primary("▶ Ver"))));
        inv.setItem(SLOT_LEAVE, createItem(Material.RED_CONCRETE,
                TierMenuColors.bold(TierMenuColors.error("✖ SAIR DA FILA")),
                List.of(TierMenuColors.separator(), TierMenuColors.neutral("/leave ou clique aqui"))));
        inv.setItem(SLOT_STATS, createItem(Material.BOOK,
                TierMenuColors.bold(TierMenuColors.white("📊 STATS")),
                List.of(TierMenuColors.neutral("Estatísticas"), "", TierMenuColors.primary("▶ Ver"))));

        return inv;
    }

    /** Actualiza slots dinâmicos sem reabrir o inventário (menos lag). */
    public void refreshQueueSlots(Player player, Inventory inv) {
        if (player == null || inv == null || inv.getSize() < 54) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Optional<GameModeId> queued = queueService.getQueuedMode(uuid);
        for (GameModeId mode : modeRegistry.enabledModes()) {
            inv.setItem(modeRegistry.guiSlot(mode), createModeItem(uuid, mode, queued.orElse(null)));
        }
    }

    private ItemStack createModeItem(UUID uuid, GameModeId mode, GameModeId activeQueue) {
        String modeId = mode.id();
        boolean queued = mode == activeQueue;
        int inQueue = queueService.getQueueSize(mode);

        List<String> lore = new ArrayList<>();
        lore.add(TierMenuColors.separator());
        lore.add(TierMenuColors.neutral("Na queue: ") + TierMenuColors.white(String.valueOf(inQueue)));
        lore.add(TierModeVisual.queueStatusLine(inQueue, queued));
        lore.add("");

        if (TierSpaceBridge.isInPlacement(uuid, modeId)) {
            lore.add(TierMenuColors.accent(TierSpaceBridge.getPlacementLabel(uuid, modeId)));
        } else {
            CompetitiveTier tier = CompetitiveTier.getTierFromRating(
                    Math.max(TierRankUtil.defaultRating(), TierSpaceBridge.getRating(uuid, modeId)));
            lore.add(TierMenuColors.neutral("Tier ") + tier.displayName());
            lore.add(TierMenuColors.neutral("Elo ") + TierModeVisual.color(mode) + formatRating(uuid, modeId));
            lore.add(TierMenuColors.neutral("K/D ") + TierMenuColors.white(
                    TierSpaceBridge.getKills(uuid, modeId) + "/" + TierSpaceBridge.getDeaths(uuid, modeId)));
        }

        lore.add("");
        if (queued) {
            lore.add(TierMenuColors.bold(TierMenuColors.success("✔ NA FILA")));
            lore.add(TierMenuColors.neutral("Duplo-clique = refresh"));
        } else {
            lore.add(TierMenuColors.bold(TierModeVisual.color(mode) + "▶ ENTRAR NA FILA"));
        }

        ItemStack item = createItem(
                mode.iconMaterial(),
                TierModeVisual.bold(TierModeVisual.color(mode)) + mode.icon() + " " + modeRegistry.displayName(mode),
                lore
        );
        if (queued) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private String formatRating(UUID uuid, String modeId) {
        int rating = TierSpaceBridge.getRating(uuid, modeId);
        return rating > 0 ? String.valueOf(rating) : String.valueOf(TierRankUtil.defaultRating());
    }

    private void fillBackground(Inventory inv) {
        ItemStack dark = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack accent = pane(Material.PURPLE_STAINED_GLASS_PANE);
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

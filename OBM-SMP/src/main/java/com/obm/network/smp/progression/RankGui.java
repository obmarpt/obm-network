package com.obm.network.smp.progression;

import com.obm.network.core.integration.EpicBroadcastBridge;
import com.obm.network.core.ui.PlayerUx;
import com.obm.network.smp.service.EconomyService;
import com.obm.network.smp.util.GuiItems;
import com.obm.network.smp.util.GuiTitles;
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

public final class RankGui {

    public static final int SIZE = 54;
    private static final String ID_PREFIX = "?8#rank:";

    public static final int SLOT_INFO = 4;
    public static final int SLOT_CLOSE = 49;

    private static final int[] RANK_SLOTS = {
            19, 20, 21, 22, 23, 24, 25, 26, 27,
            30, 31, 32
    };

    private final RankCatalog catalog;
    private final RankService rankService;
    private final EconomyService economyService;

    public RankGui(RankCatalog catalog, RankService rankService, EconomyService economyService) {
        this.catalog = catalog;
        this.rankService = rankService;
        this.economyService = economyService;
    }

    public void open(Player player) {
        List<RankDefinition> ranks = catalog.getRanks();
        RankDefinition current = rankService.getRank(player.getUniqueId());
        int currentIndex = catalog.indexOf(current.id());
        var nextOpt = catalog.getNextRank(current.id());

        Inventory inv = Bukkit.createInventory(null, SIZE, GuiTitles.RANK_PROGRESSION);
        fillBackground(inv);

        for (int i = 0; i < ranks.size() && i < RANK_SLOTS.length; i++) {
            RankDefinition rank = ranks.get(i);
            RankState state = resolveState(i, currentIndex, nextOpt.map(RankDefinition::id).orElse(null));
            inv.setItem(RANK_SLOTS[i], buildRankItem(rank, state, player, nextOpt.orElse(null)));
        }

        inv.setItem(SLOT_INFO, buildInfoItem(player, current, nextOpt.orElse(null)));
        inv.setItem(SLOT_CLOSE, GuiItems.named(Material.BARRIER, "?c?lFechar", "?7Fecha o menu de ranks"));

        player.openInventory(inv);
    }

    public boolean isRankTitle(String title) {
        return GuiTitles.RANK_PROGRESSION.equals(title);
    }

    public String extractRankId(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return null;
        }
        for (String line : item.getItemMeta().getLore()) {
            if (line != null && line.startsWith(ID_PREFIX)) {
                return line.substring(ID_PREFIX.length());
            }
        }
        return null;
    }

    public boolean isNextRank(Player player, String rankId) {
        if (rankId == null) {
            return false;
        }
        RankDefinition current = rankService.getRank(player.getUniqueId());
        return catalog.getNextRank(current.id())
                .map(next -> next.id().equals(rankId))
                .orElse(false);
    }

    public void celebrate(Player player, RankDefinition rank) {
        String display = rank.displayName();
        EpicBroadcastBridge.smpRankUp(player, display);
        PlayerUx.success(player, "Subiste para ?e" + display + "?a!");
    }

    private ItemStack buildRankItem(RankDefinition rank, RankState state, Player player, RankDefinition next) {
        Material material = switch (state) {
            case UNLOCKED -> Material.GRAY_STAINED_GLASS_PANE;
            case CURRENT -> Material.LIME_STAINED_GLASS_PANE;
            case NEXT -> Material.GOLD_BLOCK;
            case LOCKED -> Material.BLACK_STAINED_GLASS_PANE;
        };

        String name = switch (state) {
            case UNLOCKED -> "?7?m" + rank.displayName();
            case CURRENT -> "?a?l? " + rank.displayName();
            case NEXT -> "?6?l? " + rank.displayName();
            case LOCKED -> "?8?l? " + rank.displayName();
        };

        List<String> lore = new ArrayList<>();
        lore.add(ID_PREFIX + rank.id());
        lore.add("");

        switch (state) {
            case UNLOCKED -> {
                lore.add("?7J? desbloqueado");
                lore.add("?8Perks activos neste tier");
                appendPerks(lore, rank);
            }
            case CURRENT -> {
                lore.add("?a?lRank atual");
                lore.add("?7Est?s neste n?vel");
                appendPerks(lore, rank);
            }
            case NEXT -> {
                lore.add("?6?lPr?ximo rank");
                lore.add("?7Custo: ?e" + economyService.format(rank.cost()) + " Money");
                lore.add("");
                lore.add("?7Perks ao subir:");
                appendPerks(lore, rank);
                lore.add("");
                int balance = economyService.getBalance(player.getUniqueId());
                if (economyService.canAfford(player.getUniqueId(), rank.cost())) {
                    lore.add("?a?l? Clica para comprar!");
                } else {
                    lore.add("?cMoney insuficiente");
                    lore.add("?7Tens: ?f" + economyService.format(balance));
                    lore.add("?7Faltam: ?c" + economyService.format(rank.cost() - balance));
                }
            }
            case LOCKED -> {
                lore.add("?8Bloqueado");
                lore.add("?7Desbloqueia ranks anteriores primeiro");
                if (rank.cost() > 0) {
                    lore.add("?7Custo: ?8" + economyService.format(rank.cost()) + " Money");
                }
            }
        }

        ItemStack item = GuiItems.named(material, name, lore.toArray(new String[0]));
        if (state == RankState.CURRENT || state == RankState.NEXT) {
            applyGlow(item);
        }
        return item;
    }

    private ItemStack buildInfoItem(Player player, RankDefinition current, RankDefinition next) {
        List<String> lore = new ArrayList<>();
        lore.add("?7Rank: ?e" + current.displayName());
        lore.add("?7Money: ?f" + economyService.format(economyService.getBalance(player.getUniqueId())));
        lore.add("");
        lore.add("?7B?nus activos:");
        appendPerks(lore, current);
        if (next != null) {
            lore.add("");
            lore.add("?7Pr?ximo: ?e" + next.displayName());
            lore.add("?7Custo: ?f" + economyService.format(next.cost()) + " Money");
        } else {
            lore.add("");
            lore.add("?6?lRank m?ximo!");
        }
        return GuiItems.named(Material.NETHER_STAR, "?a?lA tua progress?o", lore.toArray(new String[0]));
    }

    private void appendPerks(List<String> lore, RankDefinition rank) {
        if (rank.moneyBoost() > 0) {
            lore.add("?a+ " + pct(rank.moneyBoost()) + " money gain");
        }
        if (rank.sellBoost() > 0) {
            lore.add("?2+ " + pct(rank.sellBoost()) + " venda");
        }
        if (rank.killBoost() > 0) {
            lore.add("?c+ " + pct(rank.killBoost()) + " kills");
        }
        if (rank.shopDiscount() > 0) {
            lore.add("?b- " + pct(rank.shopDiscount()) + " loja");
        }
        if (rank.extraHomes() > 1) {
            lore.add("?e+ " + rank.extraHomes() + " homes");
        }
        if (rank.cooldownReduction() > 0) {
            lore.add("?3- " + pct(rank.cooldownReduction()) + " cooldowns");
        }
        if (rank.chatPrefix() != null && !rank.chatPrefix().isBlank()) {
            lore.add("?dPrefixo: " + rank.chatPrefix());
        }
    }

    private static RankState resolveState(int index, int currentIndex, String nextId) {
        if (index < currentIndex) {
            return RankState.UNLOCKED;
        }
        if (index == currentIndex) {
            return RankState.CURRENT;
        }
        if (nextId != null && index == currentIndex + 1) {
            return RankState.NEXT;
        }
        return RankState.LOCKED;
    }

    private static void fillBackground(Inventory inv) {
        ItemStack filler = GuiItems.named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            boolean rankSlot = false;
            for (int slot : RANK_SLOTS) {
                if (slot == i) {
                    rankSlot = true;
                    break;
                }
            }
            if (!rankSlot && i != SLOT_INFO && i != SLOT_CLOSE) {
                inv.setItem(i, filler);
            }
        }
    }

    private static void applyGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private static void sendActionBar(Player player, String message) {
        try {
            net.md_5.bungee.api.chat.TextComponent tc = new net.md_5.bungee.api.chat.TextComponent(message);
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, tc);
        } catch (NoClassDefFoundError | NoSuchMethodError ignored) {
            player.sendMessage(message);
        }
    }

    private static String pct(double value) {
        return String.format("%.0f%%", value * 100);
    }

    private enum RankState {
        UNLOCKED,
        CURRENT,
        NEXT,
        LOCKED
    }
}

package com.obm.network.tierspace.ui;

import com.obm.network.tierspace.match.RematchService;
import com.obm.network.tierspace.mode.GameModeId;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Post-Match GUI — 9 slots (1×9)
 * Slot 2 Jogar · 4 Resultado · 6 Stats · 8 Lobby
 */
public class PostMatchGui {

    public static final int SLOT_PLAY_AGAIN = 2;
    public static final int SLOT_RESULT = 4;
    public static final int SLOT_STATS = 6;
    public static final int SLOT_LOBBY = 8;

    /** Compatibilidade listener rematch */
    public static final int SLOT_REMATCH = 1;

    public static boolean isPostMatchTitle(String title) {
        return title != null && (title.contains("VITÓRIA") || title.contains("DERROTA"));
    }

    public static String titleFor(PostMatchSnapshot snapshot) {
        return snapshot.won()
                ? TierMenuColors.bold(TierMenuColors.hex(TierMenuColors.WIN, "✔ VITÓRIA"))
                : TierMenuColors.bold(TierMenuColors.hex(TierMenuColors.LOSS, "✘ DERROTA"));
    }

    public static Inventory create(GameModeId mode, RematchService rematchService,
                                 org.bukkit.entity.Player viewer, PostMatchSnapshot snapshot) {
        String title = titleFor(snapshot);
        Inventory inv = Bukkit.createInventory(null, 9, title);
        fillBackground(inv, snapshot.won());

        Optional<RematchService.LastMatch> lastMatch = rematchService.getLastMatch(viewer.getUniqueId());
        String opponentName = lastMatch.map(RematchService.LastMatch::opponentName).orElse("oponente");

        String deltaLine = snapshot.delta() >= 0
                ? TierMenuColors.success("+" + snapshot.delta() + " rating")
                : TierMenuColors.error(String.valueOf(snapshot.delta()) + " rating");

        inv.setItem(SLOT_RESULT, createItem(
                snapshot.won() ? Material.NETHER_STAR : Material.BARRIER,
                snapshot.won()
                        ? TierMenuColors.bold(TierMenuColors.hex(TierMenuColors.WIN, "VITÓRIA"))
                        : TierMenuColors.bold(TierMenuColors.hex(TierMenuColors.LOSS, "DERROTA")),
                List.of(
                        TierMenuColors.neutral("Modo ") + TierMenuColors.white(mode.displayName()),
                        deltaLine,
                        TierMenuColors.neutral("Rating ") + TierMenuColors.primary(String.valueOf(snapshot.newRating())),
                        TierMenuColors.neutral("Rank ") + snapshot.rankLabel(),
                        TierMenuColors.neutral("Streak ") + TierMenuColors.accent(String.valueOf(snapshot.newStreak()))
                )
        ));

        inv.setItem(SLOT_PLAY_AGAIN, createItem(
                Material.IRON_SWORD,
                TierMenuColors.bold(TierMenuColors.primary("🔁 JOGAR OUTRA")),
                List.of(
                        TierMenuColors.neutral("Modo ") + TierMenuColors.white(mode.displayName()),
                        "",
                        TierMenuColors.primary("▶ Entrar na fila")
                )
        ));

        inv.setItem(SLOT_REMATCH, createItem(
                Material.ARROW,
                TierMenuColors.bold(TierMenuColors.accent("REMATCH")),
                List.of(
                        TierMenuColors.neutral("vs ") + TierMenuColors.white(opponentName),
                        lastMatch.isPresent()
                                ? TierMenuColors.success("Disponível")
                                : TierMenuColors.error("Indisponível")
                )
        ));

        inv.setItem(SLOT_STATS, createItem(
                Material.BOOK,
                TierMenuColors.bold(TierMenuColors.white("📊 STATS")),
                List.of(TierMenuColors.neutral("Ver estatísticas TierSpace"))
        ));

        inv.setItem(SLOT_LOBBY, createItem(
                Material.RED_BED,
                TierMenuColors.bold(TierMenuColors.neutral("🏠 LOBBY")),
                List.of(TierMenuColors.neutral("Voltar ao lobby"))
        ));

        return inv;
    }

    public static void open(org.bukkit.entity.Player player, GameModeId mode,
                            RematchService rematchService, PostMatchSnapshot snapshot) {
        player.openInventory(create(mode, rematchService, player, snapshot));
    }

    private static void fillBackground(Inventory inv, boolean won) {
        Material paneMat = won ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack pane = new ItemStack(paneMat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§8");
            pane.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (i != SLOT_PLAY_AGAIN && i != SLOT_RESULT && i != SLOT_STATS && i != SLOT_LOBBY && i != SLOT_REMATCH) {
                inv.setItem(i, pane);
            }
        }
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

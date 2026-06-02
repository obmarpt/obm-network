package com.obm.network.tierspace.listener;

import com.obm.network.tierspace.match.MatchService;
import com.obm.network.tierspace.match.RematchService;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.ui.PostMatchGui;
import com.obm.network.tierspace.ui.TierGuiMenu;
import com.obm.network.tierspace.ui.TierTopMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class TierGuiListener implements Listener {

    private final TierGuiMenu tierGuiMenu;
    private final ModeRegistry modeRegistry;
    private final MatchService matchService;
    private final RematchService rematchService;

    public TierGuiListener(TierGuiMenu tierGuiMenu,
                           ModeRegistry modeRegistry,
                           MatchService matchService,
                           RematchService rematchService) {
        this.tierGuiMenu = tierGuiMenu;
        this.modeRegistry = modeRegistry;
        this.matchService = matchService;
        this.rematchService = rematchService;
    }

    @EventHandler
    public void onTierMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (TierGuiMenu.TITLE.equals(title)) {
            handleMainMenu(event, player);
            return;
        }

        if (event.getView().getTitle().startsWith(TierTopMenu.TOP_RATING_TITLE)) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
                player.openInventory(tierGuiMenu.create(player));
            }
            return;
        }

        if (event.getView().getTitle().startsWith(TierTopMenu.TOP_STREAK_TITLE)) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
                player.openInventory(tierGuiMenu.create(player));
            }
            return;
        }

        if (PostMatchGui.isPostMatchTitle(title)) {
            handlePostMatch(event, player);
        }
    }

    private void handleMainMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot == 53) {
            player.closeInventory();
            player.performCommand("tierspace stats");
            return;
        }
        if (slot == TierGuiMenu.SLOT_TOP_RATING) {
            player.openInventory(TierTopMenu.createTopRating(GameModeId.SWORD));
            return;
        }
        if (slot == TierGuiMenu.SLOT_TOP_STREAK) {
            player.openInventory(TierTopMenu.createTopStreak(GameModeId.SWORD));
            return;
        }
        if (slot == TierGuiMenu.SLOT_SEASON_INFO || slot == TierGuiMenu.SLOT_SEASON) {
            return;
        }

        for (GameModeId mode : modeRegistry.enabledModes()) {
            if (modeRegistry.guiSlot(mode) == slot) {
                player.closeInventory();
                matchService.joinQueue(player, mode);
                return;
            }
        }
    }

    private void handlePostMatch(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        GameModeId mode = matchService.getPostMatchMode(player.getUniqueId());
        int slot = event.getRawSlot();

        if (slot == PostMatchGui.SLOT_PLAY_AGAIN) {
            player.closeInventory();
            matchService.clearPostMatchMode(player.getUniqueId());
            rematchService.clear(player.getUniqueId());
            matchService.requeue(player, mode);
            return;
        }

        if (slot == PostMatchGui.SLOT_REMATCH) {
            RematchService.RematchResult result = rematchService.requestRematch(player);
            if (result.status() == RematchService.RematchResult.Status.WAITING) {
                player.sendMessage("§eRematch pedido... à espera de §f" + result.opponentName());
            } else if (result.status() == RematchService.RematchResult.Status.READY) {
                Player opponent = Bukkit.getPlayer(result.opponentId());
                if (opponent == null || !opponent.isOnline()) {
                    player.sendMessage("§cOponente offline.");
                    return;
                }
                player.closeInventory();
                matchService.clearPostMatchMode(player.getUniqueId());
                rematchService.clear(player.getUniqueId());
                rematchService.clear(result.opponentId());
                matchService.startDirectMatch(player, opponent, result.mode());
            } else {
                player.sendMessage("§cRematch indisponível.");
            }
            return;
        }

        if (slot == PostMatchGui.SLOT_STATS) {
            player.closeInventory();
            player.performCommand("tierspace stats");
            return;
        }

        if (slot == PostMatchGui.SLOT_LOBBY) {
            player.closeInventory();
            matchService.clearPostMatchMode(player.getUniqueId());
            rematchService.clear(player.getUniqueId());
            matchService.teleportToLobby(player);
        }
    }
}

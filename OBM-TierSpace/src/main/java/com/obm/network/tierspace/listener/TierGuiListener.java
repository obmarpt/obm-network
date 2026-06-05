package com.obm.network.tierspace.listener;

import com.obm.network.tierspace.kit.KitPreference;
import com.obm.network.tierspace.kit.PlayerKitService;
import com.obm.network.tierspace.match.MatchService;
import com.obm.network.tierspace.match.RematchService;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.party.PartyService;
import com.obm.network.tierspace.queue.QueueFeedbackService;
import com.obm.network.tierspace.queue.QueueService;
import com.obm.network.tierspace.ui.KitManageGui;
import com.obm.network.tierspace.ui.PartyGui;
import com.obm.network.tierspace.ui.PostMatchGui;
import com.obm.network.tierspace.ui.TierGuiMenu;
import com.obm.network.tierspace.ui.TierTopMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

public class TierGuiListener implements Listener {

    private final TierGuiMenu tierGuiMenu;
    private final ModeRegistry modeRegistry;
    private final MatchService matchService;
    private final RematchService rematchService;
    private final QueueService queueService;
    private final QueueFeedbackService queueFeedbackService;
    private final PartyGui partyGui;
    private final PartyService partyService;
    private final KitManageGui kitManageGui;
    private final PlayerKitService playerKitService;

    public TierGuiListener(TierGuiMenu tierGuiMenu,
                           ModeRegistry modeRegistry,
                           MatchService matchService,
                           RematchService rematchService,
                           QueueService queueService,
                           QueueFeedbackService queueFeedbackService,
                           PartyGui partyGui,
                           PartyService partyService,
                           KitManageGui kitManageGui,
                           PlayerKitService playerKitService) {
        this.tierGuiMenu = tierGuiMenu;
        this.modeRegistry = modeRegistry;
        this.matchService = matchService;
        this.rematchService = rematchService;
        this.queueService = queueService;
        this.queueFeedbackService = queueFeedbackService;
        this.partyGui = partyGui;
        this.partyService = partyService;
        this.kitManageGui = kitManageGui;
        this.playerKitService = playerKitService;
    }

    @EventHandler
    public void onTierMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (TierGuiMenu.TITLE.equals(title)) {
            handleQueueDuels(event, player);
            return;
        }
        if (PartyGui.TITLE.equals(title)) {
            handlePartyGui(event, player);
            return;
        }
        if (KitManageGui.TITLE.equals(title)) {
            handleKitGui(event, player);
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

    private void handleQueueDuels(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
            return;
        }
        int slot = event.getRawSlot();

        if (slot == TierGuiMenu.SLOT_LEAVE) {
            leaveQueue(player);
            player.openInventory(tierGuiMenu.create(player));
            return;
        }
        if (slot == TierGuiMenu.SLOT_KIT) {
            player.openInventory(kitManageGui.create(player));
            return;
        }
        if (slot == TierGuiMenu.SLOT_STATS) {
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

        for (GameModeId mode : modeRegistry.enabledModes()) {
            if (modeRegistry.guiSlot(mode) != slot) {
                continue;
            }
            boolean doubleClick = event.getClick() == ClickType.DOUBLE_CLICK;
            matchService.switchQueue(player, mode);
            if (!doubleClick) {
                player.closeInventory();
            } else {
                player.openInventory(tierGuiMenu.create(player));
            }
            return;
        }
    }

    private void handlePartyGui(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == PartyGui.SLOT_BACK) {
            player.closeInventory();
            return;
        }
        if (slot == PartyGui.SLOT_CREATE && !partyService.inParty(player.getUniqueId())) {
            partyService.create(player);
            player.openInventory(partyGui.create(player));
            return;
        }
        if (slot == PartyGui.SLOT_LEAVE && partyService.inParty(player.getUniqueId())) {
            partyService.leave(player.getUniqueId());
            player.openInventory(partyGui.create(player));
            return;
        }
        if (slot >= PartyGui.SLOT_INVITE_START && slot <= 43
                && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) event.getCurrentItem().getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null && meta.getOwningPlayer().getPlayer() != null) {
                partyService.invite(player, meta.getOwningPlayer().getPlayer());
            }
        }
    }

    private void handleKitGui(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }
        int slot = event.getRawSlot();
        UUID uuid = player.getUniqueId();
        if (slot == KitManageGui.SLOT_DEFAULT) {
            playerKitService.setPreference(uuid, KitPreference.DEFAULT);
            player.sendMessage("§dKit §8| §7Modo: §fkit do modo");
            player.openInventory(kitManageGui.create(player));
        } else if (slot == KitManageGui.SLOT_CUSTOM) {
            playerKitService.setPreference(uuid, KitPreference.CUSTOM);
            player.sendMessage("§dKit §8| §7Modo: §akit personalizado");
            player.openInventory(kitManageGui.create(player));
        } else if (slot == KitManageGui.SLOT_RANDOM) {
            playerKitService.setPreference(uuid, KitPreference.RANDOM);
            player.sendMessage("§dKit §8| §7Modo: §dkit aleatório");
            player.openInventory(kitManageGui.create(player));
        } else if (slot == KitManageGui.SLOT_SAVE) {
            playerKitService.saveCustomKit(player);
            player.sendMessage("§aKit guardado.");
            player.openInventory(kitManageGui.create(player));
        } else if (slot == KitManageGui.SLOT_BACK) {
            player.openInventory(tierGuiMenu.create(player));
        }
    }

    private void leaveQueue(Player player) {
        if (queueService.leave(player.getUniqueId())) {
            queueFeedbackService.stopTracking(player.getUniqueId());
            player.sendMessage("§dTierSpace §8| §7Saíste da fila.");
        } else {
            player.sendMessage("§cNão estás em fila.");
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

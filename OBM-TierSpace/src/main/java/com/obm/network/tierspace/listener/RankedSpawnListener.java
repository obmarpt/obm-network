package com.obm.network.tierspace.listener;

import com.obm.network.core.state.PlayerStateBridge;
import com.obm.network.tierspace.TierSpacePlugin;
import com.obm.network.tierspace.hub.TierSpaceHub;
import com.obm.network.tierspace.hub.TierSpaceHubService;
import com.obm.network.tierspace.ui.PartyGui;
import com.obm.network.tierspace.ui.RankedHubItems;
import com.obm.network.tierspace.ui.TierGuiMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public final class RankedSpawnListener implements Listener {

    private final TierSpacePlugin plugin;
    private final TierSpaceHubService hubService;
    private final TierGuiMenu tierGuiMenu;
    private final PartyGui partyGui;

    public RankedSpawnListener(TierSpacePlugin plugin,
                               TierSpaceHubService hubService,
                               TierGuiMenu tierGuiMenu,
                               PartyGui partyGui) {
        this.plugin = plugin;
        this.hubService = hubService;
        this.tierGuiMenu = tierGuiMenu;
        this.partyGui = partyGui;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleHubCheck(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        hubService.handleTierSpaceLeave(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (TierSpaceHub.isInTierSpaceHub(event.getFrom().getName())) {
            hubService.handleTierSpaceLeave(player);
        }
        scheduleHubCheck(player);
    }

    private void scheduleHubCheck(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (TierSpaceHub.isInTierSpaceHub(player)) {
                hubService.handleTierSpaceJoin(player);
                PlayerStateBridge.syncFromWorld(player);
            } else {
                hubService.handleTierSpaceLeave(player);
                PlayerStateBridge.syncFromWorld(player);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!TierSpaceHub.isInTierSpaceHub(event.getPlayer())) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !RankedHubItems.isHubItem(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (RankedHubItems.isQueueSword(item)) {
            player.openInventory(tierGuiMenu.create(player));
            return;
        }
        if (RankedHubItems.isPartyItem(item)) {
            player.openInventory(partyGui.create(player));
            return;
        }
        if (RankedHubItems.isLeaveItem(item)) {
            boolean left = plugin.getQueueService().leave(player.getUniqueId());
            if (left) {
                plugin.getQueueFeedbackService().stopTracking(player.getUniqueId());
                player.sendMessage("§dTierSpace §8| §7Saíste da fila.");
            } else {
                player.sendMessage("§cNão estás em fila.");
            }
            hubService.handleTierSpaceJoin(player, true);
            plugin.getMatchService().leaveQueueState(player);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (TierSpaceHub.isInTierSpaceHub(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}

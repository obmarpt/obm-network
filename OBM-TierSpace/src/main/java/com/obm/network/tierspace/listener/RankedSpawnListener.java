package com.obm.network.tierspace.listener;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.tierspace.TierSpacePlugin;
import com.obm.network.tierspace.queue.QueueFeedbackService;
import com.obm.network.tierspace.queue.QueueService;
import com.obm.network.tierspace.ui.PartyGui;
import com.obm.network.tierspace.ui.RankedHubItems;
import com.obm.network.tierspace.ui.RankedSelectorItem;
import com.obm.network.tierspace.ui.TierGuiMenu;
import com.obm.network.tierspace.ui.TierMenuColors;
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
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class RankedSpawnListener implements Listener {

    private final TierSpacePlugin plugin;
    private final TierGuiMenu tierGuiMenu;
    private final PartyGui partyGui;
    private final QueueService queueService;
    private final QueueFeedbackService queueFeedbackService;
    private final WorldModeService worldModeService;

    public RankedSpawnListener(TierSpacePlugin plugin,
                               TierGuiMenu tierGuiMenu,
                               PartyGui partyGui,
                               QueueService queueService,
                               QueueFeedbackService queueFeedbackService) {
        this.plugin = plugin;
        this.tierGuiMenu = tierGuiMenu;
        this.partyGui = partyGui;
        this.queueService = queueService;
        this.queueFeedbackService = queueFeedbackService;
        OBMCorePlugin core = OBMCorePlugin.get();
        this.worldModeService = core != null ? core.getWorldModeService() : null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleHubSetup(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (worldModeService != null && worldModeService.isRanked(event.getFrom().getName())) {
            leaveQueueIfNeeded(player);
            removeHubItems(player);
        }
        scheduleHubSetup(player);
    }

    private void scheduleHubSetup(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (worldModeService != null && worldModeService.isRanked(player.getWorld().getName())) {
                applyRankedHub(player);
            } else {
                leaveQueueIfNeeded(player);
                removeHubItems(player);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (worldModeService == null || !worldModeService.isRanked(event.getPlayer().getWorld().getName())) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
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
        if (RankedSelectorItem.isSelector(item)) {
            player.openInventory(tierGuiMenu.create(player));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (worldModeService == null || !worldModeService.isRanked(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (RankedHubItems.isHubItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    private void applyRankedHub(Player player) {
        removeHubItems(player);

        int queueSlot = plugin.getConfig().getInt("ranked-spawn.queue-item.slot", 0);
        int partySlot = plugin.getConfig().getInt("ranked-spawn.party-item.slot", 1);
        int selectorSlot = plugin.getConfig().getInt("ranked-spawn.selector.slot", 4);

        String queueName = plugin.getConfig().getString("ranked-spawn.queue-item.name",
                TierMenuColors.bold("§9⚔ ") + TierMenuColors.primary("QUEUE DUELS"));
        player.getInventory().setItem(clampHotbar(queueSlot), RankedHubItems.queueSword(queueName, List.of(
                TierMenuColors.separator(),
                TierMenuColors.neutral("Modos PvP competitivos"),
                TierMenuColors.primary("▶ Abrir filas"),
                RankedHubItems.MARKER_QUEUE
        )));

        String partyName = plugin.getConfig().getString("ranked-spawn.party-item.name",
                TierMenuColors.bold(TierMenuColors.accent("👥 PARTY")));
        boolean inParty = plugin.getPartyService() != null && plugin.getPartyService().inParty(player.getUniqueId());
        player.getInventory().setItem(clampHotbar(partySlot), RankedHubItems.partyItem(partyName, List.of(
                TierMenuColors.separator(),
                inParty ? TierMenuColors.success("Gerir party") : TierMenuColors.neutral("Criar party"),
                TierMenuColors.primary("▶ Clique"),
                RankedHubItems.MARKER_PARTY
        )));

        if (plugin.getConfig().getBoolean("ranked-spawn.selector.enabled", true)) {
            String name = plugin.getConfig().getString("ranked-spawn.selector.name",
                    TierMenuColors.bold(TierMenuColors.primary("TierSpace")) + " §8| §7Menu");
            player.getInventory().setItem(clampHotbar(selectorSlot), RankedSelectorItem.create(name, List.of(
                    TierMenuColors.neutral("Atalho ao menu"),
                    TierMenuColors.primary("▶ Clique"),
                    RankedSelectorItem.MARKER
            )));
        }
    }

    private static int clampHotbar(int slot) {
        return Math.max(0, Math.min(8, slot));
    }

    private void leaveQueueIfNeeded(Player player) {
        if (queueService.leave(player.getUniqueId())) {
            queueFeedbackService.stopTracking(player.getUniqueId());
        }
    }

    private void removeHubItems(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (RankedHubItems.isHubItem(stack)) {
                player.getInventory().setItem(i, null);
            }
        }
    }
}

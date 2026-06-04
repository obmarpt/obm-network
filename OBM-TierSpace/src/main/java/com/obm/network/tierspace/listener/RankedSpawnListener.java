package com.obm.network.tierspace.listener;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.tierspace.TierSpacePlugin;
import com.obm.network.tierspace.ui.RankedSelectorItem;
import com.obm.network.tierspace.ui.TierGuiMenu;
import com.obm.network.tierspace.ui.TierMenuColors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

/**
 * Hub rankedSpawn: item selector abre {@link TierGuiMenu}; fila só pelo menu/NPC/comando.
 */
public final class RankedSpawnListener implements Listener {

    private final TierSpacePlugin plugin;
    private final TierGuiMenu tierGuiMenu;
    private final WorldModeService worldModeService;

    public RankedSpawnListener(TierSpacePlugin plugin, TierGuiMenu tierGuiMenu) {
        this.plugin = plugin;
        this.tierGuiMenu = tierGuiMenu;
        OBMCorePlugin core = OBMCorePlugin.get();
        this.worldModeService = core != null ? core.getWorldModeService() : null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleHubSetup(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        scheduleHubSetup(event.getPlayer());
    }

    private void scheduleHubSetup(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (worldModeService != null && worldModeService.isRanked(player.getWorld().getName())) {
                applyRankedHub(player);
            } else {
                removeSelector(player);
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
        if (!RankedSelectorItem.isSelector(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        player.openInventory(tierGuiMenu.create(player));
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (worldModeService == null || !worldModeService.isRanked(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (RankedSelectorItem.isSelector(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    private void applyRankedHub(Player player) {
        if (!plugin.getConfig().getBoolean("ranked-spawn.selector.enabled", true)) {
            return;
        }

        removeSelector(player);

        int slot = plugin.getConfig().getInt("ranked-spawn.selector.slot", 4);
        slot = Math.max(0, Math.min(8, slot));

        String name = plugin.getConfig().getString(
                "ranked-spawn.selector.name",
                TierMenuColors.bold(TierMenuColors.primary("TierSpace")) + " §8| §7Modos");
        List<String> lore = List.of(
                TierMenuColors.neutral("Escolhe Sword, NoDebuff, UHC..."),
                TierMenuColors.primary("▶ Clique para abrir"),
                RankedSelectorItem.MARKER
        );

        player.getInventory().setItem(slot, RankedSelectorItem.create(name, lore));
    }

    private void removeSelector(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (RankedSelectorItem.isSelector(stack)) {
                player.getInventory().setItem(i, null);
            }
        }
    }
}

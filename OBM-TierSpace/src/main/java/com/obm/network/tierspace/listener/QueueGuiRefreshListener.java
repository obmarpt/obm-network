package com.obm.network.tierspace.listener;

import com.obm.network.tierspace.TierSpacePlugin;
import com.obm.network.tierspace.ui.TierGuiMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

public final class QueueGuiRefreshListener implements Listener {

    private final TierSpacePlugin plugin;
    private final TierGuiMenu tierGuiMenu;
    private BukkitTask task;

    public QueueGuiRefreshListener(TierSpacePlugin plugin, TierGuiMenu tierGuiMenu) {
        this.plugin = plugin;
        this.tierGuiMenu = tierGuiMenu;
    }

    public void start() {
        long ticks = Math.max(20L, plugin.getConfig().getLong("ranked-spawn.gui-refresh-ticks", 40L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getOpenInventory().getTitle().equals(TierGuiMenu.TITLE)) {
                    continue;
                }
                var top = player.getOpenInventory().getTopInventory();
                if (top != null && top.getSize() >= 54) {
                    tierGuiMenu.refreshQueueSlots(player, top);
                }
            }
        }, ticks, ticks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }
}

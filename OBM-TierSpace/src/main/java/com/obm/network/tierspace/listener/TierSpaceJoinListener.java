package com.obm.network.tierspace.listener;

import com.obm.network.tierspace.TierSpacePlugin;
import com.obm.network.tierspace.hub.TierSpaceHub;
import com.obm.network.tierspace.reward.RankRewardService;
import com.obm.network.tierspace.season.TierSeasonManager;
import com.obm.network.tierspace.ui.TierSpaceTabService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TierSpaceJoinListener implements Listener {

    private final TierSpaceTabService tabService;
    private final TierSeasonManager seasonManager;
    private final RankRewardService rankRewardService;

    public TierSpaceJoinListener(TierSpaceTabService tabService,
                                 TierSeasonManager seasonManager,
                                 RankRewardService rankRewardService) {
        this.tabService = tabService;
        this.seasonManager = seasonManager;
        this.rankRewardService = rankRewardService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        seasonManager.ensurePlayerSeason(event.getPlayer());
        rankRewardService.refresh(event.getPlayer());
        tabService.refresh(event.getPlayer());
        TierSpacePlugin plugin = TierSpacePlugin.get();
        if (plugin != null && TierSpaceHub.isInTierSpaceHub(event.getPlayer())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (event.getPlayer().isOnline() && plugin.getHubService() != null) {
                    plugin.getHubService().handleTierSpaceJoin(event.getPlayer());
                }
            }, 5L);
        }
    }
}

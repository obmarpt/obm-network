package com.obm.network.smp.progression;

import org.bukkit.entity.Player;

import java.util.UUID;

public class ProgressionBonusService {

    private final RankService rankService;
    private final LevelService levelService;

    public ProgressionBonusService(RankService rankService, LevelService levelService) {
        this.rankService = rankService;
        this.levelService = levelService;
    }

    public int applyKillReward(UUID uuid, int baseReward) {
        double multiplier = rankService.getKillMultiplier(uuid) * levelService.getLevelKillMultiplier(uuid);
        return Math.max(0, (int) Math.round(baseReward * multiplier));
    }

    public int applySellPayout(UUID uuid, int baseTotal) {
        double multiplier = rankService.getSellMultiplier(uuid) * levelService.getLevelSellMultiplier(uuid);
        return Math.max(0, (int) Math.floor(baseTotal * multiplier));
    }

    public int applyShopPrice(UUID uuid, int basePrice) {
        return rankService.applyShopDiscount(uuid, basePrice);
    }

    public RankService getRankService() {
        return rankService;
    }

    public LevelService getLevelService() {
        return levelService;
    }
}

package com.obm.network.smp.progression;

import com.obm.network.smp.permission.SmpPermissions;
import org.bukkit.Bukkit;
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
        int amount = Math.max(0, (int) Math.round(baseReward * multiplier));
        return applyVipCoinBonus(uuid, amount);
    }

    public int applySellPayout(UUID uuid, int baseTotal) {
        double multiplier = rankService.getSellMultiplier(uuid) * levelService.getLevelSellMultiplier(uuid);
        int amount = Math.max(0, (int) Math.floor(baseTotal * multiplier));
        return applyVipCoinBonus(uuid, amount);
    }

    public int applyPlaytimeReward(UUID uuid, int baseReward) {
        return applyVipCoinBonus(uuid, Math.max(0, baseReward));
    }

    private int applyVipCoinBonus(UUID uuid, int amount) {
        if (amount <= 0) {
            return 0;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return amount;
        }
        return Math.max(0, (int) Math.round(amount * SmpPermissions.vipCoinMultiplier(player)));
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

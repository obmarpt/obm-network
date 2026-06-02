package com.obm.network.smp.progression;

import com.obm.network.smp.service.EconomyService;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RankService {

    public record RankPurchaseResult(boolean success, String message) {
        public static RankPurchaseResult ok(String message) {
            return new RankPurchaseResult(true, message);
        }

        public static RankPurchaseResult fail(String message) {
            return new RankPurchaseResult(false, message);
        }
    }

    private final RankCatalog catalog;
    private final PlayerProgressionStore store;
    private final EconomyService economyService;

    public RankService(RankCatalog catalog, PlayerProgressionStore store, EconomyService economyService) {
        this.catalog = catalog;
        this.store = store;
        this.economyService = economyService;
    }

    public RankDefinition getRank(UUID uuid) {
        String rankId = store.getRankId(uuid, catalog.getDefaultRankId());
        return catalog.getRank(rankId).orElseGet(() -> catalog.getRank(catalog.getDefaultRankId())
                .orElse(new RankDefinition(catalog.getDefaultRankId(), "Bronze", 0, 0, 0, 0)));
    }

    public RankPurchaseResult purchaseNextRank(Player player) {
        UUID uuid = player.getUniqueId();
        RankDefinition current = getRank(uuid);
        var nextOptional = catalog.getNextRank(current.id());
        if (nextOptional.isEmpty()) {
            return RankPurchaseResult.fail("Já possuis o rank máximo (§e" + current.displayName() + "§c).");
        }

        RankDefinition next = nextOptional.get();
        if (!economyService.canAfford(uuid, next.cost())) {
            return RankPurchaseResult.fail("Precisas de §e" + economyService.format(next.cost())
                    + " §cpara comprar o rank §e" + next.displayName() + "§c.");
        }

        if (!economyService.safeWithdraw(uuid, next.cost())) {
            return RankPurchaseResult.fail("Não foi possível processar o pagamento.");
        }

        store.setRankId(uuid, next.id());
        return RankPurchaseResult.ok("Rank atualizado para §e" + next.displayName()
                + "§a! Bónus: §7+" + percent(next.sellBoost()) + " venda, +"
                + percent(next.killBoost()) + " kills, -" + percent(next.shopDiscount()) + " loja.");
    }

    public double getSellMultiplier(UUID uuid) {
        return 1.0 + getRank(uuid).sellBoost();
    }

    public double getKillMultiplier(UUID uuid) {
        return 1.0 + getRank(uuid).killBoost();
    }

    public double getShopDiscount(UUID uuid) {
        return Math.min(0.75, Math.max(0, getRank(uuid).shopDiscount()));
    }

    public int applyShopDiscount(UUID uuid, int price) {
        double discount = getShopDiscount(uuid);
        return Math.max(1, (int) Math.ceil(price * (1.0 - discount)));
    }

    public RankCatalog getCatalog() {
        return catalog;
    }

    private String percent(double value) {
        return String.format("%.0f%%", value * 100);
    }
}

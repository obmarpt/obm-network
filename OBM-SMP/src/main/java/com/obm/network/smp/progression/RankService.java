package com.obm.network.smp.progression;

import com.obm.network.smp.service.EconomyService;
import org.bukkit.Sound;
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
        String rankId = catalog.normalizeRankId(store.getRankId(uuid, catalog.getDefaultRankId()));
        return catalog.getRank(rankId).orElseGet(() -> catalog.getRank(catalog.getDefaultRankId())
                .orElse(new RankDefinition(catalog.getDefaultRankId(), "Rookie", 0,
                        0, 0, 0, 0, 1, 0, "§7[Rookie]")));
    }

    public RankPurchaseResult purchaseNextRank(Player player) {
        return purchaseNextRank(player, true);
    }

    public RankPurchaseResult purchaseNextRank(Player player, boolean feedback) {
        UUID uuid = player.getUniqueId();
        RankDefinition current = getRank(uuid);
        var nextOptional = catalog.getNextRank(current.id());
        if (nextOptional.isEmpty()) {
            return RankPurchaseResult.fail("Já possuis o rank máximo (§e" + current.displayName() + "§c).");
        }

        RankDefinition next = nextOptional.get();
        if (next.cost() <= 0) {
            return RankPurchaseResult.fail("Rank inválido.");
        }

        if (!economyService.canAfford(uuid, next.cost())) {
            return RankPurchaseResult.fail("Precisas de §e" + economyService.format(next.cost())
                    + " §cpara o rank §e" + next.displayName() + "§c.");
        }

        if (!com.obm.network.core.security.SecurityBridge.tryClaimEconomy(
                uuid, "smp_rankup", next.cost(), next.id())) {
            return RankPurchaseResult.fail("Transação duplicada bloqueada.");
        }

        if (!economyService.withdraw(uuid, next.cost()).transactionSuccess()) {
            return RankPurchaseResult.fail("Não foi possível processar o pagamento.");
        }

        store.setRankId(uuid, next.id());
        if (feedback) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            player.sendTitle("§6§lRANK UP!", "§7Agora és §e" + next.displayName(), 5, 50, 15);
        }

        return RankPurchaseResult.ok("§aRank §e" + next.displayName() + " §adesbloqueado!"
                + " §7| §a+" + percent(next.moneyBoost()) + " money"
                + " §7| §e" + next.extraHomes() + " homes"
                + (next.cooldownReduction() > 0 ? " §7| §b-" + percent(next.cooldownReduction()) + " cooldowns" : "")
                + (next.chatPrefix() != null && !next.chatPrefix().isBlank()
                ? " §7| prefixo §f" + next.chatPrefix() : ""));
    }

    public double getMoneyMultiplier(UUID uuid) {
        RankDefinition rank = getRank(uuid);
        return 1.0 + rank.moneyBoost();
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

    public int getExtraHomes(UUID uuid) {
        return Math.max(1, getRank(uuid).extraHomes());
    }

    public double getCooldownReduction(UUID uuid) {
        return Math.min(0.5, Math.max(0, getRank(uuid).cooldownReduction()));
    }

    public String getChatPrefix(UUID uuid) {
        String prefix = getRank(uuid).chatPrefix();
        return prefix != null ? prefix : "";
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

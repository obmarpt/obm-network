package com.obm.network.smp.progression;

public record RankDefinition(
        String id,
        String displayName,
        int cost,
        double moneyBoost,
        double sellBoost,
        double killBoost,
        double shopDiscount,
        int extraHomes,
        double cooldownReduction,
        String chatPrefix
) {
    public double totalMoneyMultiplier() {
        return 1.0 + moneyBoost + sellBoost * 0.5 + killBoost * 0.5;
    }
}

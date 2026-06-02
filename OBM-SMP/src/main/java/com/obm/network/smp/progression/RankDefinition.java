package com.obm.network.smp.progression;

public record RankDefinition(
        String id,
        String displayName,
        int cost,
        double sellBoost,
        double killBoost,
        double shopDiscount
) {
}

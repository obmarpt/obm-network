package com.obm.network.tierspace.rating;

import com.obm.network.core.tier.TierRank;
import com.obm.network.core.tier.TierRankUtil;

public record RatingChange(
        int delta,
        int baseDelta,
        int dailyBonus,
        int oldRating,
        int newRating,
        TierRank oldRank,
        TierRank newRank,
        double streakMultiplier,
        int streakForBonus,
        int newStreak,
        int lostStreak,
        boolean demotionShielded,
        int newRatingLossStreak
) {
    public static RatingChange win(int baseDelta, int dailyBonus, int oldRating, int newRating,
                                   double streakMultiplier, int streakForBonus, int newStreak) {
        TierRank oldRank = TierRankUtil.fromRating(oldRating);
        TierRank newRank = TierRankUtil.fromRating(newRating);
        return new RatingChange(
                baseDelta + dailyBonus,
                baseDelta,
                dailyBonus,
                oldRating,
                newRating,
                oldRank,
                newRank,
                streakMultiplier,
                streakForBonus,
                newStreak,
                0,
                false,
                0
        );
    }

    public static RatingChange loss(int delta, int oldRating, int newRating, int lostStreak,
                                    boolean demotionShielded, int newRatingLossStreak) {
        TierRank oldRank = TierRankUtil.fromRating(oldRating);
        TierRank newRank = TierRankUtil.fromRating(newRating);
        return new RatingChange(
                delta,
                delta,
                0,
                oldRating,
                newRating,
                oldRank,
                newRank,
                1.0,
                0,
                0,
                lostStreak,
                demotionShielded,
                newRatingLossStreak
        );
    }

    public boolean promoted() {
        return TierRankUtil.compare(newRank, oldRank) > 0;
    }

    public boolean demoted() {
        return TierRankUtil.compare(newRank, oldRank) < 0;
    }

    public boolean hadStreakOnLoss() {
        return lostStreak > 0;
    }

    public boolean hadStreakBonus() {
        return streakMultiplier > 1.0;
    }

    public String streakMultiplierLabel() {
        return String.format("%.1f", streakMultiplier);
    }
}

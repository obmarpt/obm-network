package com.obm.network.tierspace.ui;

import com.obm.network.tierspace.rating.RatingChange;

public record PostMatchSnapshot(
        boolean won,
        int delta,
        int newRating,
        String rankLabel,
        int newStreak,
        boolean inPlacement
) {
    public static PostMatchSnapshot win(RatingChange change, boolean inPlacement) {
        String rank = inPlacement ? "§ePlacement" : change.newRank().displayName();
        return new PostMatchSnapshot(
                true,
                change.delta(),
                change.newRating(),
                rank,
                change.newStreak(),
                inPlacement
        );
    }

    public static PostMatchSnapshot defeat(RatingChange change, boolean inPlacement) {
        String rank = inPlacement ? "§ePlacement" : change.newRank().displayName();
        return new PostMatchSnapshot(
                false,
                change.delta(),
                change.newRating(),
                rank,
                change.newStreak(),
                inPlacement
        );
    }
}

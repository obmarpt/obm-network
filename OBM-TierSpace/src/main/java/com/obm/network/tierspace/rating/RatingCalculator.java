package com.obm.network.tierspace.rating;

import com.obm.network.core.tier.TierRankUtil;

public final class RatingCalculator {

    private final int kFactorNew;
    private final int kFactorEstablished;
    private final int establishedGames;
    private final int placementKFactor;
    private final double streakBonusPerWin;
    private final int streakBonusCap;
    private final boolean demotionShieldEnabled;
    private final int demotionShieldLosses;
    private final int winMin;
    private final int winMax;
    private final int lossMin;
    private final int lossMax;

    public RatingCalculator(int kFactorNew, int kFactorEstablished, int establishedGames,
                            int placementKFactor, double streakBonusPerWin, int streakBonusCap,
                            boolean demotionShieldEnabled, int demotionShieldLosses,
                            int winMin, int winMax, int lossMin, int lossMax) {
        this.kFactorNew = kFactorNew;
        this.kFactorEstablished = kFactorEstablished;
        this.establishedGames = establishedGames;
        this.placementKFactor = placementKFactor;
        this.streakBonusPerWin = streakBonusPerWin;
        this.streakBonusCap = streakBonusCap;
        this.demotionShieldEnabled = demotionShieldEnabled;
        this.demotionShieldLosses = demotionShieldLosses;
        this.winMin = winMin;
        this.winMax = winMax;
        this.lossMin = lossMin;
        this.lossMax = lossMax;
    }

    public RatingChange calculateWin(int winnerRating, int loserRating, int winnerGamesPlayed,
                                   int preWinStreak, boolean inPlacement) {
        int rawDelta = calculateDelta(winnerRating, loserRating, winnerGamesPlayed, 1.0, inPlacement);
        int clamped = clampWinDelta(rawDelta, winnerRating, loserRating, inPlacement);
        clamped = Math.max(1, clamped);

        int streakForBonus = Math.min(Math.max(preWinStreak + 1, 1), streakBonusCap);
        double streakMultiplier = 1.0 + streakBonusPerWin * streakForBonus;
        int boostedDelta = (int) Math.round(clamped * streakMultiplier);
        boostedDelta = Math.max(winMin, Math.min(winMax + 5, boostedDelta));

        int newRating = Math.max(0, winnerRating + boostedDelta);
        return RatingChange.win(
                boostedDelta,
                0,
                winnerRating,
                newRating,
                streakMultiplier,
                streakForBonus,
                preWinStreak + 1
        );
    }

    public RatingChange applyDailyBonus(RatingChange change, int dailyBonus) {
        if (dailyBonus <= 0) {
            return change;
        }
        int newRating = change.newRating() + dailyBonus;
        return RatingChange.win(
                change.baseDelta() + dailyBonus,
                dailyBonus,
                change.oldRating(),
                newRating,
                change.streakMultiplier(),
                change.streakForBonus(),
                change.newStreak()
        );
    }

    public RatingChange calculateLoss(int loserRating, int winnerRating, int loserGamesPlayed,
                                      int lostStreak, int ratingLossStreak, boolean inPlacement) {
        int delta = calculateDelta(loserRating, winnerRating, loserGamesPlayed, 0.0, inPlacement);
        delta = clampLossDelta(delta, loserRating, winnerRating, inPlacement);

        int newRating = Math.max(0, loserRating + delta);
        boolean demotionShielded = false;
        int newRatingLossStreak = 0;

        if (demotionShieldEnabled && TierRankUtil.wouldDemote(loserRating, newRating)) {
            newRatingLossStreak = ratingLossStreak + 1;
            if (newRatingLossStreak < demotionShieldLosses) {
                newRating = TierRankUtil.getDivisionFloor(loserRating);
                demotionShielded = true;
            } else {
                newRatingLossStreak = 0;
            }
        }

        int actualDelta = newRating - loserRating;
        return RatingChange.loss(actualDelta, loserRating, newRating, lostStreak, demotionShielded, newRatingLossStreak);
    }

    private int clampWinDelta(int rawDelta, int playerRating, int opponentRating, boolean inPlacement) {
        if (inPlacement) {
            return Math.max(winMin, Math.min(winMax, Math.max(1, rawDelta)));
        }
        int delta = Math.max(winMin, Math.min(winMax, Math.max(1, Math.abs(rawDelta))));
        int diff = opponentRating - playerRating;
        if (diff >= 150) {
            delta = Math.min(winMax, delta + 4);
        } else if (diff <= -150) {
            delta = Math.max(winMin, delta - 3);
        }
        return delta;
    }

    private int clampLossDelta(int rawDelta, int loserRating, int winnerRating, boolean inPlacement) {
        if (inPlacement) {
            return -Math.max(lossMin, Math.min(lossMax, Math.max(1, Math.abs(rawDelta))));
        }
        int magnitude = Math.max(lossMin, Math.min(lossMax, Math.max(1, Math.abs(rawDelta))));
        int diff = winnerRating - loserRating;
        if (diff >= 150) {
            magnitude = Math.min(lossMax, magnitude + 3);
        } else if (diff <= -150) {
            magnitude = Math.max(lossMin, magnitude - 2);
        }
        return -magnitude;
    }

    private int calculateDelta(int playerRating, int opponentRating, int gamesPlayed,
                               double actualScore, boolean inPlacement) {
        double expected = expectedScore(playerRating, opponentRating);
        int k = inPlacement ? placementKFactor
                : (gamesPlayed < establishedGames ? kFactorNew : kFactorEstablished);
        return (int) Math.round(k * (actualScore - expected));
    }

    private double expectedScore(int ratingA, int ratingB) {
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }
}

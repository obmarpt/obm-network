package com.obm.network.smp.duel;

import java.util.UUID;

public final class Duel {

    private final String id;
    private final UUID playerOne;
    private final UUID playerTwo;
    private final DuelReturnPoint returnOne;
    private final DuelReturnPoint returnTwo;
    private final DuelItemLedger ledgerOne;
    private final DuelItemLedger ledgerTwo;
    private final int maxRounds;
    private final int winsNeeded;

    private DuelState state;
    private final long startedAt;
    private int roundsWonOne;
    private int roundsWonTwo;
    private int currentRound;
    private long roundStartedAt;

    public Duel(String id, UUID playerOne, UUID playerTwo,
                DuelReturnPoint returnOne, DuelReturnPoint returnTwo,
                int maxRounds) {
        this.id = id;
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.returnOne = returnOne;
        this.returnTwo = returnTwo;
        this.ledgerOne = new DuelItemLedger();
        this.ledgerTwo = new DuelItemLedger();
        this.maxRounds = Math.max(1, maxRounds);
        this.winsNeeded = (this.maxRounds / 2) + 1;
        this.state = DuelState.PENDING;
        this.startedAt = System.currentTimeMillis();
        this.currentRound = 0;
    }

    public String id() {
        return id;
    }

    public UUID playerOne() {
        return playerOne;
    }

    public UUID playerTwo() {
        return playerTwo;
    }

    public DuelReturnPoint returnOne() {
        return returnOne;
    }

    public DuelReturnPoint returnTwo() {
        return returnTwo;
    }

    public int maxRounds() {
        return maxRounds;
    }

    public int winsNeeded() {
        return winsNeeded;
    }

    public int roundsWonOne() {
        return roundsWonOne;
    }

    public int roundsWonTwo() {
        return roundsWonTwo;
    }

    public int roundsWon(UUID uuid) {
        if (playerOne.equals(uuid)) {
            return roundsWonOne;
        }
        if (playerTwo.equals(uuid)) {
            return roundsWonTwo;
        }
        return 0;
    }

    public DuelState state() {
        return state;
    }

    public void setState(DuelState state) {
        this.state = state;
    }

    public long startedAt() {
        return startedAt;
    }

    public int currentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public long roundStartedAt() {
        return roundStartedAt;
    }

    public void setRoundStartedAt(long roundStartedAt) {
        this.roundStartedAt = roundStartedAt;
    }

    public void addRoundWin(UUID winnerId) {
        if (playerOne.equals(winnerId)) {
            roundsWonOne++;
        } else if (playerTwo.equals(winnerId)) {
            roundsWonTwo++;
        }
    }

    public boolean hasMatchWinner() {
        return roundsWonOne >= winsNeeded || roundsWonTwo >= winsNeeded;
    }

    public UUID matchWinner() {
        if (roundsWonOne >= winsNeeded) {
            return playerOne;
        }
        if (roundsWonTwo >= winsNeeded) {
            return playerTwo;
        }
        return null;
    }

    public boolean involves(UUID uuid) {
        return playerOne.equals(uuid) || playerTwo.equals(uuid);
    }

    public UUID opponent(UUID uuid) {
        if (playerOne.equals(uuid)) {
            return playerTwo;
        }
        if (playerTwo.equals(uuid)) {
            return playerOne;
        }
        return null;
    }

    public DuelReturnPoint returnFor(UUID uuid) {
        if (playerOne.equals(uuid)) {
            return returnOne;
        }
        if (playerTwo.equals(uuid)) {
            return returnTwo;
        }
        return null;
    }

    DuelItemLedger ledgerFor(UUID uuid) {
        if (playerOne.equals(uuid)) {
            return ledgerOne;
        }
        if (playerTwo.equals(uuid)) {
            return ledgerTwo;
        }
        return null;
    }
}

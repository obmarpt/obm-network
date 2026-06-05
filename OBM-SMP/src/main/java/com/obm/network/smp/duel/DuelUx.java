package com.obm.network.smp.duel;

import com.obm.network.core.ui.PlayerUx;
import com.obm.network.core.ui.VisualFeedback;
import com.obm.network.core.ui.VisualFeedbackScene;
import org.bukkit.entity.Player;

final class DuelUx {

    private DuelUx() {
    }

    static String roundLabel(Duel duel, int round) {
        if (round >= duel.maxRounds()
                || (duel.roundsWonOne() == 1 && duel.roundsWonTwo() == 1)) {
            return "FINAL ROUND";
        }
        return "ROUND " + round;
    }

    static String scoreLine(Duel duel) {
        return duel.roundsWonOne() + " - " + duel.roundsWonTwo();
    }

    static void sendActionBar(Player player, Duel duel) {
        if (player == null || !player.isOnline()) {
            return;
        }
        PlayerUx.actionBar(player, "§6" + scoreLine(duel));
    }

    static void sendRoundStart(Player player, String roundLabel) {
        if (player == null || !player.isOnline()) {
            return;
        }
        VisualFeedback.play(player, VisualFeedbackScene.DUEL_ROUND,
                "§6§l" + roundLabel, "§7Prepara-te...");
    }

    static void sendCountdownTick(Player player, int secondsLeft) {
        if (player == null || !player.isOnline()) {
            return;
        }
        VisualFeedback.playInstant(player, VisualFeedbackScene.DUEL_COUNTDOWN,
                "§e§l" + secondsLeft, "");
    }

    static void sendFightStart(Player player, String roundLabel, Duel duel) {
        if (player == null || !player.isOnline()) {
            return;
        }
        VisualFeedback.play(player, VisualFeedbackScene.DUEL_FIGHT,
                "§6§l" + roundLabel, "§a§lFIGHT!");
        sendActionBar(player, duel);
    }

    static void sendRoundWin(Player winner, Player loser, Duel duel) {
        String winnerName = winner != null ? winner.getName() : "?";
        if (winner != null && winner.isOnline()) {
            VisualFeedback.play(winner, VisualFeedbackScene.DUEL_ROUND_WIN,
                    null, "§f" + winnerName + " §7venceu o round");
            sendActionBar(winner, duel);
        }
        if (loser != null && loser.isOnline()) {
            VisualFeedback.play(loser, VisualFeedbackScene.DUEL_ROUND_LOST,
                    null, "§f" + winnerName + " §7venceu o round");
            sendActionBar(loser, duel);
        }
    }

    static void sendMatchVictory(Player winner, Player loser) {
        String winnerName = winner != null ? winner.getName() : "?";
        if (winner != null && winner.isOnline()) {
            VisualFeedback.play(winner, VisualFeedbackScene.DUEL_VICTORY,
                    null, "§f" + winnerName + " §7venceu o duelo");
        }
        if (loser != null && loser.isOnline()) {
            VisualFeedback.play(loser, VisualFeedbackScene.DUEL_DEFEAT,
                    null, "§f" + winnerName + " §7venceu o duelo");
        }
    }

    static void sendDuelStart(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        VisualFeedback.play(player, VisualFeedbackScene.DUEL_START);
    }
}

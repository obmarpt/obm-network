package com.obm.network.lobby.join;

import com.obm.network.core.ui.PlayerUx;
import com.obm.network.core.ui.VisualFeedback;
import com.obm.network.core.ui.VisualFeedbackScene;
import org.bukkit.entity.Player;

/**
 * Primeira impressão no lobby — title, som e mensagem curta.
 */
public final class WelcomeMessage {

    private WelcomeMessage() {
    }

    public static void send(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        VisualFeedback.play(player, VisualFeedbackScene.SERVER_JOIN);

        player.sendMessage("");
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7Usa a §e§lBússola §7ou clica num §fNPC §7para escolher modo.");
        player.sendMessage("§7Comandos: §e/help §7· §e/menu §7· §e/daily §7· §e/link");
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        PlayerUx.hint(player, "§7Rush Money = ranks/loja · Emeralds = rede global");
    }
}

package com.obm.network.lobby.join;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Primeira impressão no lobby — apenas UX (title + chat).
 */
public final class WelcomeMessage {

    private WelcomeMessage() {
    }

    public static void send(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        player.sendTitle(
                "§b§l✦ MineSpace ✦",
                "§7Bem-vindo, §f" + player.getName(),
                10, 50, 15
        );

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);

        player.sendMessage("");
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7Bem-vindo ao §b§lMineSpace§7, §f" + player.getName() + "§7!");
        player.sendMessage("");
        player.sendMessage("§7⚔ §6Rush §8• §c💀 Hardcore §8• §b🏆 TierSpace");
        player.sendMessage("§a§l→ §aUsa §e/menu §apara começar");
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
    }
}

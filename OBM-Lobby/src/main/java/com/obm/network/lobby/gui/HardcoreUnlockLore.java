package com.obm.network.lobby.gui;

import com.obm.network.core.economy.CurrencyLabels;
import com.obm.network.core.hardcore.HardcoreUnlockService;
import com.obm.network.core.hardcore.HardcoreUnlockStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formatação visual de lore Hardcore (apenas apresentação).
 */
final class HardcoreUnlockLore {

    static final String SEP = "§8§m━━━━━━━━━━━━━━";

    private HardcoreUnlockLore() {
    }

    static List<String> lockedHeaderLore(HardcoreUnlockStatus unlock, int lives) {
        List<String> lore = new ArrayList<>();
        lore.add(SEP);
        lore.add("§c🔒 Bloqueado");
        lore.add("");
        lore.add("§7Requisitos:");
        lore.add("§fLevel: §e" + unlock.requiredLevel());
        lore.add("§f" + CurrencyLabels.SMP_MONEY + ": §e" + formatCoinsComma(unlock.requiredCoins()));
        lore.add("");
        lore.add("§7Teu progresso:");
        lore.add(progressLevel(unlock.playerLevel(), unlock.requiredLevel()));
        lore.add(progressCoins(unlock.playerCoins(), unlock.requiredCoins()));
        lore.add("");
        lore.add("§7Vidas: §c" + lives);
        return lore;
    }

    static List<String> unlockedHeaderLore(int lives) {
        List<String> lore = new ArrayList<>();
        lore.add(SEP);
        lore.add("§a✅ Desbloqueado");
        lore.add("");
        lore.add("§7Vidas: §c" + lives);
        lore.add("");
        lore.add("§e§l👉 Clica para jogar");
        return lore;
    }

    static List<String> mainMenuLockedLore(HardcoreUnlockStatus unlock) {
        List<String> lore = new ArrayList<>();
        lore.add("§c🔒 Bloqueado");
        lore.add("");
        lore.add("§7Requisitos:");
        lore.add("§fLevel: §e" + unlock.requiredLevel());
        lore.add("§f" + CurrencyLabels.SMP_MONEY + ": §e" + formatCoinsComma(unlock.requiredCoins()));
        lore.add("");
        lore.add("§7Progresso:");
        lore.add(progressLevel(unlock.playerLevel(), unlock.requiredLevel()));
        lore.add(progressCoins(unlock.playerCoins(), unlock.requiredCoins()));
        return lore;
    }

    static String progressLevel(int current, int required) {
        String color = current >= required ? "§a" : "§e";
        return "§fLevel: " + color + current + "§7/§e" + required;
    }

    static String progressCoins(int current, int required) {
        String cur = HardcoreUnlockService.formatCoins(current);
        String req = HardcoreUnlockService.formatCoins(required);
        String color = current >= required ? "§a" : "§6";
        return "§f" + CurrencyLabels.SMP_MONEY + ": " + color + cur + "§7/§e" + req;
    }

    static String formatCoinsComma(int amount) {
        return String.format(Locale.US, "%,d", amount);
    }
}

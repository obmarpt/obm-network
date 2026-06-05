package com.obm.network.smp.service;

import com.obm.network.smp.progression.LevelService;
import com.obm.network.smp.progression.ProgressionBonusService;
import com.obm.network.smp.retention.RetentionFeedback;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class SellService {

    public record SellResult(boolean success, String message, int total) {
        public static SellResult ok(String message, int total) {
            return new SellResult(true, message, total);
        }

        public static SellResult fail(String message) {
            return new SellResult(false, message, 0);
        }
    }

    private final SellCatalog catalog;
    private final EconomyService economyService;
    private final ProgressionBonusService bonusService;
    private final LevelService levelService;

    public SellService(SellCatalog catalog, EconomyService economyService,
                       ProgressionBonusService bonusService, LevelService levelService) {
        this.catalog = catalog;
        this.economyService = economyService;
        this.bonusService = bonusService;
        this.levelService = levelService;
    }

    public SellCatalog getCatalog() {
        return catalog;
    }

    public int calculateBaseValue(ItemStack[] items) {
        int total = 0;
        if (items == null) {
            return 0;
        }
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            int price = catalog.getPrice(item.getType());
            if (price > 0) {
                total += price * item.getAmount();
            }
        }
        return total;
    }

    public int calculateValue(Player player, ItemStack[] items) {
        return bonusService.applySellPayout(player.getUniqueId(), calculateBaseValue(items));
    }

    public SellResult sellItems(Player player, ItemStack[] items) {
        int baseTotal = 0;
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            int unit = catalog.getPrice(item.getType());
            if (unit > 0) {
                baseTotal += unit * item.getAmount();
            }
        }

        if (baseTotal <= 0) {
            return SellResult.fail("§cNão há itens vendáveis. Consulta §e/sell §ce coloca materiais com preço.");
        }

        int total = bonusService.applySellPayout(player.getUniqueId(), baseTotal);
        economyService.deposit(player.getUniqueId(), total);
        RetentionFeedback.coinsGained(player, total, "Sell");

        levelService.addSellXp(player, total);

        String boostInfo = total > baseTotal
                ? " §7(§a+" + (total - baseTotal) + " bónus rank/nível§7)" : "";
        return SellResult.ok("§aVenda confirmada: §e"
                + com.obm.network.core.economy.CurrencyLabels.formatSmpMoney(total) + boostInfo + "§a.", total);
    }
}

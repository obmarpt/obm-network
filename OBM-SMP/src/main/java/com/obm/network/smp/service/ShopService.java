package com.obm.network.smp.service;

import com.obm.network.smp.permission.SmpPermissions;
import com.obm.network.smp.progression.ProgressionBonusService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ShopService {

    public record PurchaseResult(boolean success, String message) {
        public static PurchaseResult ok(String message) {
            return new PurchaseResult(true, message);
        }

        public static PurchaseResult fail(String message) {
            return new PurchaseResult(false, message);
        }
    }

    private final ShopCatalog catalog;
    private final EconomyService economyService;
    private final ProgressionBonusService bonusService;

    public ShopService(ShopCatalog catalog, EconomyService economyService, ProgressionBonusService bonusService) {
        this.catalog = catalog;
        this.economyService = economyService;
        this.bonusService = bonusService;
    }

    public ShopCatalog getCatalog() {
        return catalog;
    }

    public int getUnitPrice(Player player, String category, Material material) {
        int base = catalog.getPrice(category, material);
        return bonusService.applyShopPrice(player.getUniqueId(), base);
    }

    public PurchaseResult purchase(Player player, String category, Material material, int quantity) {
        if (quantity <= 0 || quantity > 64) {
            return PurchaseResult.fail("§cQuantidade inválida.");
        }
        if (!catalog.canPurchase(material)) {
            return PurchaseResult.fail("§cEste item não está disponível para compra.");
        }
        if (catalog.isVipItem(material) && !SmpPermissions.hasVip(player)) {
            return PurchaseResult.fail("§cItem exclusivo VIP. §7Precisas de §6obm.smp.vip§c.");
        }

        int baseUnit = catalog.getPrice(category, material);
        if (baseUnit <= 0) {
            return PurchaseResult.fail("§cItem não encontrado na loja.");
        }

        int unitPrice = bonusService.applyShopPrice(player.getUniqueId(), baseUnit);
        int total = unitPrice * quantity;
        UUID uuid = player.getUniqueId();

        if (!economyService.canAfford(uuid, total)) {
            return PurchaseResult.fail("§cSaldo insuficiente. Precisas de §e"
                    + com.obm.network.core.economy.CurrencyLabels.formatSmpMoney(total) + "§c.");
        }

        ItemStack stack = new ItemStack(material, quantity);
        if (player.getInventory().firstEmpty() == -1 && !canStack(player, material, quantity)) {
            return PurchaseResult.fail("§cInventário cheio.");
        }

        if (!economyService.withdraw(uuid, total).transactionSuccess()) {
            return PurchaseResult.fail("§cNão foi possível processar o pagamento.");
        }

        var leftovers = player.getInventory().addItem(stack);
        if (!leftovers.isEmpty()) {
            economyService.deposit(uuid, total);
            return PurchaseResult.fail("§cInventário cheio. Compra cancelada.");
        }

        String discountInfo = unitPrice < baseUnit
                ? " §7(§a-" + (baseUnit - unitPrice) + " desconto§7)" : "";
        return PurchaseResult.ok("§aCompra confirmada: §e" + quantity + "x "
                + material.name() + " §apor §f"
                    + com.obm.network.core.economy.CurrencyLabels.formatSmpMoney(total) + discountInfo + "§a.");
    }

    private boolean canStack(Player player, Material material, int quantity) {
        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content != null && content.getType() == material
                    && content.getAmount() + quantity <= content.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }
}

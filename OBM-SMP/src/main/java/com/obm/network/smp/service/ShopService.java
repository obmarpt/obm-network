package com.obm.network.smp.service;

import com.obm.network.smp.progression.ProgressionBonusService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopService {

    public record ShopSession(String category, Material material, int quantity) {
    }

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
    private final Map<UUID, ShopSession> sessions = new ConcurrentHashMap<>();

    public ShopService(ShopCatalog catalog, EconomyService economyService, ProgressionBonusService bonusService) {
        this.catalog = catalog;
        this.economyService = economyService;
        this.bonusService = bonusService;
    }

    public ShopCatalog getCatalog() {
        return catalog;
    }

    public void setSession(UUID uuid, ShopSession session) {
        if (session == null) {
            sessions.remove(uuid);
        } else {
            sessions.put(uuid, session);
        }
    }

    public ShopSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public void clearSession(UUID uuid) {
        sessions.remove(uuid);
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

        int baseUnit = catalog.getPrice(category, material);
        if (baseUnit <= 0) {
            return PurchaseResult.fail("§cItem não encontrado na loja.");
        }

        int unitPrice = bonusService.applyShopPrice(player.getUniqueId(), baseUnit);
        int total = unitPrice * quantity;
        UUID uuid = player.getUniqueId();

        if (!economyService.canAfford(uuid, total)) {
            return PurchaseResult.fail("§cSaldo insuficiente. Precisas de §e" + total + " coins§c.");
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

        clearSession(uuid);
        String discountInfo = unitPrice < baseUnit
                ? " §7(§a-" + (baseUnit - unitPrice) + " desconto§7)" : "";
        return PurchaseResult.ok("§aCompra confirmada: §e" + quantity + "x "
                + material.name() + " §apor §f" + total + " coins" + discountInfo + "§a.");
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

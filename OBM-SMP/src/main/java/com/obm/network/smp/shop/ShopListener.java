package com.obm.network.smp.shop;

import com.obm.network.smp.SMPPlugin;
import com.obm.network.smp.permission.SmpPermissions;
import com.obm.network.smp.service.ShopCatalog;
import com.obm.network.smp.service.ShopService;
import com.obm.network.smp.shop.session.ShopSession;
import com.obm.network.smp.shop.session.ShopSessionManager;
import com.obm.network.smp.util.GuiTitles;
import com.obm.network.smp.util.GuiViewTitles;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopListener implements Listener {

    private final SMPPlugin plugin;
    private final ShopGui shopGui;
    private final ShopService shopService;
    private final ShopCatalog shopCatalog;
    private final CategoryGui categoryGui;
    private final ShopSessionManager sessionManager;
    private final SearchGui searchGui;
    private final Map<UUID, BukkitTask> pendingSessionCleanup = new ConcurrentHashMap<>();

    public ShopListener(ShopGui shopGui, ShopService shopService, ShopCatalog shopCatalog) {
        this.plugin = SMPPlugin.get();
        this.shopGui = shopGui;
        this.shopService = shopService;
        this.shopCatalog = shopCatalog;
        this.categoryGui = shopGui.getCategoryGui();
        this.sessionManager = shopGui.getSessionManager();
        this.searchGui = new SearchGui();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        cleanupShopState(event.getEntity());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupShopState(player), 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanupShopState(event.getPlayer());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!GuiViewTitles.isSmpShop(event.getView())) {
            return;
        }

        UUID uuid = player.getUniqueId();
        cancelPendingCleanup(uuid);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingSessionCleanup.remove(uuid);
            if (!player.isOnline()) {
                return;
            }
            if (GuiViewTitles.isSmpShop(player.getOpenInventory())) {
                return;
            }
            ShopSession session = sessionManager.get(uuid);
            if (session != null && session.isAwaitingSearch()) {
                return;
            }
            sessionManager.remove(uuid);
        }, 2L);
        pendingSessionCleanup.put(uuid, task);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (GuiViewTitles.isSmpShop(event.getView())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryView view = event.getView();
        if (!GuiViewTitles.isSmpShop(view)) {
            return;
        }

        event.setCancelled(true);

        if (plugin.getConfig().getBoolean("shop.debug-clicks", false)) {
            player.sendMessage("§eCLICK DETECTADO");
        }

        if (event.getClickedInventory() == null
                || event.getRawSlot() >= view.getTopInventory().getSize()) {
            return;
        }

        String title = GuiViewTitles.resolve(view);
        if (GuiTitles.SHOP_CATEGORIES.equals(title)) {
            handleCategories(event, player);
        } else if (GuiTitles.SHOP_ITEMS.equals(title)) {
            handleItems(event, player);
        }
    }

    private void handleCategories(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        if (slot == CategoryGui.SLOT_CLOSE || clicked.getType() == Material.BARRIER) {
            cleanupShopState(player);
            return;
        }
        String mainKey = CategoryGui.mainCategoryFromIcon(clicked);
        if (mainKey != null && CategoryGui.isMainCategoryKey(mainKey)) {
            if (!CategoryGui.canAccess(player, mainKey)) {
                SmpPermissions.deny(player, CategoryGui.requiredPermission(mainKey), null);
                return;
            }
            SmpPermissions.debug(player, "Loja categoria: " + mainKey);
            categoryGui.openItems(player, mainKey);
        }
    }

    private void handleItems(InventoryClickEvent event, Player player) {
        ShopSession session = sessionManager.getOrCreate(player);
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();

        if (slot == CategoryGui.SLOT_CLOSE) {
            cleanupShopState(player);
            return;
        }
        if (slot == CategoryGui.SLOT_BACK) {
            shopGui.openCategories(player);
            return;
        }
        if (slot == CategoryGui.SLOT_SEARCH) {
            searchGui.beginSearch(player, session);
            return;
        }
        if (slot == CategoryGui.SLOT_PREV && session.getPage() > 0) {
            session.setPage(session.getPage() - 1);
            categoryGui.refreshItems(player);
            return;
        }
        if (slot == CategoryGui.SLOT_NEXT) {
            session.setPage(session.getPage() + 1);
            categoryGui.refreshItems(player);
            return;
        }
        if (slot == CategoryGui.SLOT_CONFIRM) {
            handleConfirm(player, session);
            return;
        }
        if (slot > CategoryGui.ITEMS_END || clicked == null || clicked.getType().isAir()) {
            return;
        }

        Material material = clicked.getType();
        if (session.getCurrentCategory() == null) {
            player.sendMessage("§cSessão da loja expirou. Abre §e/shop §cnovamente.");
            cleanupShopState(player);
            return;
        }
        if (!isListedInCategory(session, material)) {
            return;
        }
        if (!shopCatalog.canPurchase(material)) {
            player.sendMessage("§cEste item não está disponível para compra.");
            return;
        }
        if (shopCatalog.isVipItem(material) && !SmpPermissions.hasVip(player)) {
            player.sendMessage("§cItem exclusivo VIP. §7Precisas de §6obm.smp.vip§c.");
            return;
        }
        if (!CategoryGui.canAccess(player, session.getCurrentCategory())) {
            SmpPermissions.deny(player, CategoryGui.requiredPermission(session.getCurrentCategory()), null);
            shopGui.openCategories(player);
            return;
        }

        if (session.hasSelection() && session.getSelectedMaterial() == material) {
            adjustQuantity(event, session);
            categoryGui.refreshItems(player);
            return;
        }

        session.selectMaterial(material);
        categoryGui.refreshItems(player);
    }

    private void handleConfirm(Player player, ShopSession session) {
        if (SmpPermissions.deny(player, SmpPermissions.SHOP, "Permissão: obm.smp.shop")) {
            return;
        }
        if (!session.hasSelection()) {
            player.sendMessage("§cSeleciona um item primeiro.");
            return;
        }
        String catalogCategory = categoryGui.resolveCatalogCategory(
                session.getCurrentCategory(), session.getSelectedMaterial());
        var result = shopService.purchase(
                player,
                catalogCategory,
                session.getSelectedMaterial(),
                session.getSelectedAmount());
        player.sendMessage(result.message());
        if (result.success()) {
            categoryGui.refreshItems(player);
        }
    }

    private void adjustQuantity(InventoryClickEvent event, ShopSession session) {
        ClickType type = event.getClick();
        int delta = switch (type) {
            case LEFT -> 1;
            case RIGHT -> -1;
            case SHIFT_LEFT -> 16;
            case SHIFT_RIGHT -> -16;
            default -> 0;
        };
        if (delta != 0) {
            session.adjustAmount(delta);
        }
    }

    private boolean isListedInCategory(ShopSession session, Material material) {
        return SearchGui.filterEntries(
                shopCatalog,
                categoryGui.catalogKeysFor(session.getCurrentCategory()),
                session.getSearchQuery()).stream()
                .anyMatch(e -> e.getKey() == material);
    }

    private void cleanupShopState(Player player) {
        if (player == null) {
            return;
        }
        cancelPendingCleanup(player.getUniqueId());
        sessionManager.remove(player.getUniqueId());
        player.closeInventory();
    }

    private void cancelPendingCleanup(UUID uuid) {
        BukkitTask task = pendingSessionCleanup.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
}

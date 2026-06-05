package com.obm.network.smp.shop;

import com.obm.network.smp.permission.SmpPermissions;
import com.obm.network.smp.service.ShopCatalog;
import com.obm.network.smp.service.ShopService;
import com.obm.network.smp.shop.session.ShopSession;
import com.obm.network.smp.shop.session.ShopSessionManager;
import com.obm.network.smp.util.GuiItems;
import com.obm.network.smp.util.GuiTitles;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CategoryGui {

    public static final int SIZE = 54;
    public static final int ITEMS_START = 0;
    public static final int ITEMS_END = 44;
    public static final int ITEMS_PER_PAGE = ITEMS_END - ITEMS_START + 1;

    public static final int SLOT_BACK = 45;
    public static final int SLOT_SEARCH = 46;
    public static final int SLOT_MINUS = 47;
    public static final int SLOT_PREV = 48;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_NEXT = 50;
    public static final int SLOT_PLUS = 51;
    public static final int SLOT_CONFIRM = 52;
    public static final int SLOT_CLOSE = 53;

    private static final int[] CATEGORY_SLOTS = {19, 21, 23, 25, 31};

    private static final LinkedHashMap<String, MainCategory> MAIN_CATEGORIES = new LinkedHashMap<>();

    static {
        MAIN_CATEGORIES.put("blocks", new MainCategory("Blocks", Material.GRASS_BLOCK,
                List.of("construcao", "decoracao", "armazenamento", "redstone"), SmpPermissions.SHOP));
        MAIN_CATEGORIES.put("farming", new MainCategory("Farming", Material.WHEAT,
                List.of("comida"), SmpPermissions.SHOP));
        MAIN_CATEGORIES.put("mining", new MainCategory("Mining", Material.DIAMOND_PICKAXE,
                List.of("mineracao"), SmpPermissions.SHOP));
        MAIN_CATEGORIES.put("combat", new MainCategory("Combat", Material.NETHERITE_SWORD,
                List.of("combate", "armaduras", "pocoes"), SmpPermissions.SHOP));
        MAIN_CATEGORIES.put("vip", new MainCategory("VIP Exclusive", Material.NETHER_STAR,
                List.of("raros"), SmpPermissions.VIP));
    }

    private final ShopCatalog catalog;
    private final ShopService shopService;
    private final ShopSessionManager sessionManager;

    public CategoryGui(ShopCatalog catalog, ShopService shopService, ShopSessionManager sessionManager) {
        this.catalog = catalog;
        this.shopService = shopService;
        this.sessionManager = sessionManager;
    }

    public void openMain(Player player) {
        ShopSession session = sessionManager.getOrCreate(player);
        session.setAwaitingSearch(false);
        Inventory inventory = Bukkit.createInventory(null, SIZE, GuiTitles.SHOP_CATEGORIES);

        int index = 0;
        for (Map.Entry<String, MainCategory> entry : MAIN_CATEGORIES.entrySet()) {
            if (index >= CATEGORY_SLOTS.length) {
                break;
            }
            MainCategory cat = entry.getValue();
            boolean allowed = canAccess(player, entry.getKey());
            Material icon = allowed ? cat.icon() : Material.BARRIER;
            String title = allowed ? "§e" + cat.displayName() : "§c" + cat.displayName();
            inventory.setItem(CATEGORY_SLOTS[index++], GuiItems.named(
                    icon,
                    title,
                    allowed ? "§7Clique para ver itens" : "§cSem permissão",
                    allowed ? "" : "§7" + cat.permission(),
                    "§8" + entry.getKey()));
        }
        inventory.setItem(SLOT_CLOSE, GuiItems.named(Material.BARRIER, "§cFechar"));
        player.openInventory(inventory);
    }

    public void openItems(Player player, String mainCategory) {
        if (!canAccess(player, mainCategory)) {
            SmpPermissions.deny(player, requiredPermission(mainCategory), null);
            return;
        }
        ShopSession session = sessionManager.getOrCreate(player);
        session.setCurrentCategory(mainCategory);
        session.setAwaitingSearch(false);
        Inventory inventory = Bukkit.createInventory(null, SIZE, GuiTitles.SHOP_ITEMS);
        fillItemsInventory(player, session, inventory);
        player.openInventory(inventory);
    }

    public void refreshItems(Player player) {
        if (!GuiTitles.SHOP_ITEMS.equals(com.obm.network.smp.util.GuiViewTitles.resolve(player.getOpenInventory()))) {
            openItems(player, sessionManager.getOrCreate(player).getCurrentCategory());
            return;
        }
        ShopSession session = sessionManager.getOrCreate(player);
        fillItemsInventory(player, session, player.getOpenInventory().getTopInventory());
    }

    public void refreshSelectionBar(Player player) {
        if (!GuiTitles.SHOP_ITEMS.equals(com.obm.network.smp.util.GuiViewTitles.resolve(player.getOpenInventory()))) {
            return;
        }
        ShopSession session = sessionManager.getOrCreate(player);
        fillBottomBar(player, session, player.getOpenInventory().getTopInventory());
        refreshSelectedGridSlots(player, session);
    }

    private void fillItemsInventory(Player player, ShopSession session, Inventory inventory) {
        String mainCategory = session.getCurrentCategory();
        MainCategory meta = MAIN_CATEGORIES.get(mainCategory);
        if (meta == null) {
            return;
        }

        List<Map.Entry<Material, Integer>> list = SearchGui.filterEntries(
                catalog, meta.catalogKeys(), session.getSearchQuery());

        int maxPage = Math.max(0, (list.size() - 1) / ITEMS_PER_PAGE);
        int safePage = Math.min(session.getPage(), maxPage);
        session.setPage(safePage);

        for (int slot = ITEMS_START; slot <= ITEMS_END; slot++) {
            inventory.setItem(slot, null);
        }

        int start = safePage * ITEMS_PER_PAGE;
        for (int slot = ITEMS_START; slot <= ITEMS_END; slot++) {
            int itemIndex = start + (slot - ITEMS_START);
            if (itemIndex >= list.size()) {
                break;
            }
            Map.Entry<Material, Integer> entry = list.get(itemIndex);
            Material material = entry.getKey();
            boolean blocked = catalog.isBlocked(material);
            int unit = shopService.getUnitPrice(player, resolveCatalogCategory(mainCategory, material), material);
            inventory.setItem(slot, SearchGui.shopItemStack(
                    material, entry.getValue(), blocked, catalog.isVipItem(material), session, unit));
        }

        fillBottomBar(player, session, inventory);
    }

    private void fillBottomBar(Player player, ShopSession session, Inventory inventory) {
        String mainCategory = session.getCurrentCategory();
        List<Map.Entry<Material, Integer>> list = SearchGui.filterEntries(
                catalog,
                catalogKeysFor(mainCategory),
                session.getSearchQuery());
        int maxPage = Math.max(0, (list.size() - 1) / ITEMS_PER_PAGE);
        int safePage = session.getPage();

        String searchHint = session.getSearchQuery().isBlank()
                ? "§7Nenhum filtro"
                : "§7Filtro: §f" + session.getSearchQuery();

        inventory.setItem(SLOT_BACK, GuiItems.named(Material.ARROW, "§eVoltar", "§7Categorias"));
        inventory.setItem(SLOT_SEARCH, GuiItems.named(Material.OAK_SIGN, "§bPesquisar",
                "§7Clica e escreve no chat",
                searchHint,
                "§8cancelar §7para limpar"));

        if (session.hasSelection()) {
            int unit = shopService.getUnitPrice(player,
                    resolveCatalogCategory(mainCategory, session.getSelectedMaterial()),
                    session.getSelectedMaterial());
            int total = unit * session.getSelectedAmount();
            int maxQty = session.getMaxQuantity();

            inventory.setItem(SLOT_MINUS, GuiItems.named(Material.RED_STAINED_GLASS_PANE, "§c− Quantidade",
                    "§7Atual: §e" + session.getSelectedAmount(),
                    "§7Mínimo: §f1"));
            inventory.setItem(SLOT_PLUS, GuiItems.named(Material.LIME_STAINED_GLASS_PANE, "§a+ Quantidade",
                    "§7Atual: §e" + session.getSelectedAmount(),
                    "§7Máximo: §f" + maxQty));
            inventory.setItem(SLOT_INFO, GuiItems.named(session.getSelectedMaterial(), "§6Seleção",
                    "§7Qtd: §e" + session.getSelectedAmount() + "x",
                    "§7Preço/un: §e" + unit + " Money",
                    "§7Total: §e" + total + " Money"));
            inventory.setItem(SLOT_CONFIRM, GuiItems.named(Material.LIME_CONCRETE, "§a§lConfirmar Compra",
                    "§7Comprar §e" + session.getSelectedAmount() + "x",
                    "§7Custo: §e" + total + " Money"));
        } else {
            inventory.setItem(SLOT_MINUS, GuiItems.named(Material.GRAY_STAINED_GLASS_PANE, "§8− Quantidade",
                    "§7Seleciona um item"));
            inventory.setItem(SLOT_PLUS, GuiItems.named(Material.GRAY_STAINED_GLASS_PANE, "§8+ Quantidade",
                    "§7Seleciona um item"));
            inventory.setItem(SLOT_INFO, GuiItems.named(Material.GRAY_STAINED_GLASS_PANE, "§7Seleção",
                    "§7Clica num item da grelha"));
            inventory.setItem(SLOT_CONFIRM, GuiItems.named(Material.GRAY_CONCRETE, "§8Confirmar Compra",
                    "§7Seleciona um item primeiro"));
        }

        if (safePage > 0) {
            inventory.setItem(SLOT_PREV, GuiItems.named(Material.SPECTRAL_ARROW, "§ePágina anterior"));
        } else {
            inventory.setItem(SLOT_PREV, null);
        }
        if (safePage < maxPage) {
            inventory.setItem(SLOT_NEXT, GuiItems.named(Material.SPECTRAL_ARROW, "§ePróxima página"));
        } else {
            inventory.setItem(SLOT_NEXT, null);
        }
        inventory.setItem(SLOT_CLOSE, GuiItems.named(Material.BARRIER, "§cFechar"));
    }

    private void refreshSelectedGridSlots(Player player, ShopSession session) {
        if (!session.hasSelection() || session.getCurrentCategory() == null) {
            return;
        }
        Inventory inventory = player.getOpenInventory().getTopInventory();
        String mainCategory = session.getCurrentCategory();
        List<Map.Entry<Material, Integer>> list = SearchGui.filterEntries(
                catalog, catalogKeysFor(mainCategory), session.getSearchQuery());
        int start = session.getPage() * ITEMS_PER_PAGE;
        Material selected = session.getSelectedMaterial();
        for (int slot = ITEMS_START; slot <= ITEMS_END; slot++) {
            int itemIndex = start + (slot - ITEMS_START);
            if (itemIndex >= list.size()) {
                break;
            }
            Map.Entry<Material, Integer> entry = list.get(itemIndex);
            if (entry.getKey() != selected) {
                continue;
            }
            Material material = entry.getKey();
            int unit = shopService.getUnitPrice(player, resolveCatalogCategory(mainCategory, material), material);
            inventory.setItem(slot, SearchGui.shopItemStack(
                    material, entry.getValue(), catalog.isBlocked(material),
                    catalog.isVipItem(material), session, unit));
            break;
        }
    }

    public static boolean isMainCategoryKey(String key) {
        return MAIN_CATEGORIES.containsKey(key);
    }

    public static String mainCategoryFromIcon(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return null;
        }
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null || lore.isEmpty()) {
            return null;
        }
        String line = lore.get(lore.size() - 1);
        if (line != null && (line.startsWith("§8") || line.startsWith("?8"))) {
            return line.substring(2);
        }
        return null;
    }

    public String resolveCatalogCategory(String mainCategory, Material material) {
        MainCategory meta = MAIN_CATEGORIES.get(mainCategory);
        if (meta == null) {
            return mainCategory;
        }
        for (String cat : meta.catalogKeys()) {
            if (catalog.getItems(cat).containsKey(material)) {
                return cat;
            }
        }
        return meta.catalogKeys().get(0);
    }

    public List<String> catalogKeysFor(String mainCategory) {
        MainCategory meta = MAIN_CATEGORIES.get(mainCategory);
        return meta == null ? List.of() : meta.catalogKeys();
    }

    public static boolean canAccess(Player player, String mainCategoryKey) {
        return SmpPermissions.has(player, requiredPermission(mainCategoryKey));
    }

    public static String requiredPermission(String mainCategoryKey) {
        MainCategory meta = MAIN_CATEGORIES.get(mainCategoryKey);
        return meta == null ? SmpPermissions.SHOP : meta.permission();
    }

    private record MainCategory(String displayName, Material icon, List<String> catalogKeys, String permission) {
    }
}

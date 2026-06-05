package com.obm.network.smp.util;

public final class GuiTitles {

    public static final String SHOP_CATEGORIES = "§6Loja §8| §7Categorias";
    public static final String SHOP_ITEMS = "§6Loja §8| §7Itens";
    /** @deprecated use {@link #SHOP_ITEMS} */
    public static final String SHOP_CATEGORY_PREFIX = "§6Loja §8| §7";
    /** @deprecated quantity is inline in item grid */
    public static final String SHOP_QUANTITY = "§6Loja §8| §7Quantidade";
    /** @deprecated confirm is inline in item grid */
    public static final String SHOP_CONFIRM = "§6Loja §8| §7Confirmar";

    public static final String SELL = "§2Vender §8| §7Itens";

    public static final String AUCTION_MAIN = "§dLeilão §8| §7Menu";
    public static final String AUCTION_BROWSE = "§dLeilão §8| §7Listagens";
    public static final String AUCTION_SELL = "§dLeilão §8| §7Anunciar";

    public static final String MARKET_PREFIX = "§6Mercado §8| §7Pág";

    public static final String RANK_PROGRESSION = "§a§lRANK PROGRESSION";

    private GuiTitles() {
    }

    public static boolean isShopCategory(String title) {
        return title != null && title.startsWith(SHOP_CATEGORY_PREFIX)
                && !title.equals(SHOP_CATEGORIES)
                && !title.equals(SHOP_QUANTITY)
                && !title.equals(SHOP_CONFIRM);
    }

    public static String categoryFromTitle(String title) {
        String raw = title.substring(SHOP_CATEGORY_PREFIX.length());
        int pageMarker = raw.indexOf(" §8(");
        if (pageMarker >= 0) {
            return raw.substring(0, pageMarker);
        }
        return raw;
    }
}

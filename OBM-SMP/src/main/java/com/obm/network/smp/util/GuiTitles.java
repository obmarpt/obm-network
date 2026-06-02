package com.obm.network.smp.util;

public final class GuiTitles {

    public static final String SHOP_CATEGORIES = "§6Loja §8| §7Categorias";
    public static final String SHOP_CATEGORY_PREFIX = "§6Loja §8| §7";
    public static final String SHOP_QUANTITY = "§6Loja §8| §7Quantidade";
    public static final String SHOP_CONFIRM = "§6Loja §8| §7Confirmar";

    public static final String SELL = "§2Vender §8| §7Itens";

    public static final String AUCTION_MAIN = "§dLeilão §8| §7Menu";
    public static final String AUCTION_BROWSE = "§dLeilão §8| §7Listagens";
    public static final String AUCTION_SELL = "§dLeilão §8| §7Anunciar";

    private GuiTitles() {
    }

    public static boolean isShopCategory(String title) {
        return title != null && title.startsWith(SHOP_CATEGORY_PREFIX)
                && !title.equals(SHOP_CATEGORIES)
                && !title.equals(SHOP_QUANTITY)
                && !title.equals(SHOP_CONFIRM);
    }

    public static String categoryFromTitle(String title) {
        return title.substring(SHOP_CATEGORY_PREFIX.length());
    }
}

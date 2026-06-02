package com.obm.network.smp.sell;

import com.obm.network.smp.service.SellService;
import com.obm.network.smp.util.GuiItems;
import com.obm.network.smp.util.GuiTitles;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SellGui {

    public static final int INPUT_START = 0;
    public static final int INPUT_END = 44;
    public static final int INFO_SLOT = 49;
    public static final int CONFIRM_SLOT = 53;
    public static final int CANCEL_SLOT = 45;

    private final SellService sellService;

    public SellGui(SellService sellService) {
        this.sellService = sellService;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, GuiTitles.SELL);
        refreshFooter(player, inventory, new ItemStack[0]);
        player.openInventory(inventory);
    }

    public void refreshFooter(Player player, Inventory inventory, ItemStack[] contents) {
        int base = sellService.calculateBaseValue(contents);
        int total = sellService.calculateValue(player, contents);
        inventory.setItem(INFO_SLOT, GuiItems.named(Material.PAPER, "§eValor estimado",
                "§7Base: §f" + base + " coins",
                "§7Com bónus: §a" + total + " coins",
                "§8Coloca itens nas slots acima"));
        inventory.setItem(CONFIRM_SLOT, GuiItems.named(Material.LIME_CONCRETE, "§aConfirmar venda", "§7Recebe §e" + total + " coins"));
        inventory.setItem(CANCEL_SLOT, GuiItems.named(Material.RED_CONCRETE, "§cCancelar", "§7Devolve os itens"));
    }
}

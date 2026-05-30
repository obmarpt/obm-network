package com.obm.network.smp.commands;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.smp.gui.RushShopMenu;
import com.obm.network.smp.service.EconomyService;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopCommand implements CommandExecutor {

    private final EconomyService economyService;
    private final Map<String, ShopItem> shopItems = createShopItems();

    public ShopCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        if (!OBMCorePlugin.get().getWorldModeService().isRush(player.getWorld().getName())) {
            player.sendMessage("§cEste comando só funciona dentro do Rush SMP.");
            return true;
        }

        if (args.length == 0) {
            player.openInventory(RushShopMenu.create(player));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> {
                sendShopList(player);
                return true;
            }
            case "buy" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUso: /shop buy <item>");
                    return true;
                }
                buyItem(player, args[1].toLowerCase());
                return true;
            }
            default -> {
                sendShopHelp(player);
                return true;
            }
        }
    }

    private void sendShopHelp(Player player) {
        player.sendMessage("§6--- Loja Rush SMP ---");
        player.sendMessage("§e/shop list §7- exibe os itens disponíveis");
        player.sendMessage("§e/shop buy <item> §7- compra um item");
    }

    private void sendShopList(Player player) {
        player.sendMessage("§6--- Itens da Loja ---");
        shopItems.values().forEach(item -> player.sendMessage(
                "§e" + item.commandName + "§7 - " + item.displayName + " §8(" + item.cost + " coins)"));
    }

    private void buyItem(Player player, String itemName) {
        ShopItem shopItem = shopItems.get(itemName);
        if (shopItem == null) {
            player.sendMessage("§cItem inválido. Use /shop list para ver os itens disponíveis.");
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!economyService.canAfford(uuid, shopItem.cost)) {
            player.sendMessage("§cSaldo insuficiente. Você precisa de §e" + shopItem.cost + " coins§c.");
            return;
        }

        ItemStack[] stacks = shopItem.items.stream().map(ItemStack::clone).toArray(ItemStack[]::new);
        if (!player.getInventory().addItem(stacks).isEmpty()) {
            player.sendMessage("§cSeu inventário está cheio.");
            return;
        }

        economyService.withdraw(uuid, shopItem.cost);
        player.sendMessage("§aCompra realizada: §e" + shopItem.displayName + " §7por §f" + shopItem.cost + " coins.");
    }

    private static Map<String, ShopItem> createShopItems() {
        Map<String, ShopItem> items = new HashMap<>();
        items.put("weapon", new ShopItem("weapon", "Espada Diamond", 2500,
                List.of(createItem(Material.DIAMOND_SWORD, "§bEspada Diamond"))));
        items.put("gapple", new ShopItem("gapple", "Gapple x5", 1500,
                List.of(createItem(Material.GOLDEN_APPLE, "§6Gapple x5", 5))));
        items.put("pearl", new ShopItem("pearl", "Ender Pearl x16", 1000,
                List.of(createItem(Material.ENDER_PEARL, "§5Ender Pearl x16", 16))));
        items.put("armor", new ShopItem("armor", "Conjunto Ferro", 5000,
                List.of(
                        createItem(Material.IRON_HELMET, "§7Capacete de Ferro"),
                        createItem(Material.IRON_CHESTPLATE, "§7Peitoral de Ferro"),
                        createItem(Material.IRON_LEGGINGS, "§7Calças de Ferro"),
                        createItem(Material.IRON_BOOTS, "§7Botas de Ferro")
                )));
        return items;
    }

    private static ItemStack createItem(Material type, String name) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createItem(Material type, String name, int amount) {
        ItemStack item = createItem(type, name);
        item.setAmount(amount);
        return item;
    }

    private static class ShopItem {
        private final String commandName;
        private final String displayName;
        private final int cost;
        private final List<ItemStack> items;

        public ShopItem(String commandName, String displayName, int cost, List<ItemStack> items) {
            this.commandName = commandName;
            this.displayName = displayName;
            this.cost = cost;
            this.items = items;
        }
    }
}

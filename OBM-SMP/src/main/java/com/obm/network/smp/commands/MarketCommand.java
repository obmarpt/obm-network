package com.obm.network.smp.commands;

import com.obm.network.smp.permission.SmpPermissions;
import com.obm.network.smp.util.SmpRateLimits;
import com.obm.network.smp.market.MarketGui;
import com.obm.network.smp.service.EconomyService;
import com.obm.network.smp.service.MarketService;
import com.obm.network.smp.service.MarketService.MarketSaleResult;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MarketCommand implements CommandExecutor {

    private final MarketGui marketGui;
    private final MarketService marketService;
    private final int maxListingsPerPlayer;
    private final int minPrice;
    private final int maxPrice;

    public MarketCommand(org.bukkit.plugin.Plugin plugin, MarketGui marketGui,
                       MarketService marketService, EconomyService economyService) {
        this.marketGui = marketGui;
        this.marketService = marketService;
        this.maxListingsPerPlayer = plugin.getConfig().getInt("market.max-listings-per-player", 10);
        this.minPrice = plugin.getConfig().getInt("market.min-price", 1);
        this.maxPrice = plugin.getConfig().getInt("market.max-price", 1000000);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }
        if (SmpPermissions.deny(player, SmpPermissions.MARKET, "Permissão: obm.smp.market")) {
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            marketGui.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell" -> {
                if (args.length != 2) {
                    player.sendMessage("§eUso: /market sell <preço>");
                    return true;
                }
                return handleSell(player, args[1]);
            }
            case "buy" -> {
                if (args.length != 2) {
                    player.sendMessage("§eUso: /market buy <id>");
                    return true;
                }
                return handleBuy(player, args[1]);
            }
            case "remove" -> {
                if (args.length != 2) {
                    player.sendMessage("§eUso: /market remove <id>");
                    return true;
                }
                return handleRemove(player, args[1]);
            }
            default -> {
                player.sendMessage("§eUso: /market [list|sell|buy|remove]");
                return true;
            }
        }
    }

    private boolean handleSell(Player player, String argPrice) {
        SmpRateLimits.MarketCheckResult rate = SmpRateLimits.checkMarketListing(player);
        if (!rate.allowed()) {
            player.sendMessage(rate.message());
            return true;
        }
        try {
            int price = Integer.parseInt(argPrice);
            MarketSaleResult result = marketService.createListing(
                    player, player.getInventory().getItemInMainHand(), price,
                    maxListingsPerPlayer, minPrice, maxPrice);
            if (result.isSuccess()) {
                SmpRateLimits.recordMarketListing(player);
                player.getInventory().setItemInMainHand(null);
                player.sendMessage(result.getMessage());
                marketGui.open(player);
            } else {
                player.sendMessage(result.getMessage());
            }
            return true;
        } catch (NumberFormatException ex) {
            player.sendMessage("§cPreço inválido. Usa um número inteiro.");
            return true;
        }
    }

    private boolean handleBuy(Player player, String id) {
        MarketSaleResult result = marketService.buyListing(player, id);
        player.sendMessage(result.getMessage());
        if (result.isSuccess()) {
            marketGui.open(player);
        }
        return true;
    }

    private boolean handleRemove(Player player, String id) {
        MarketSaleResult result = marketService.removeListing(id, player.getUniqueId());
        player.sendMessage(result.getMessage());
        if (result.isSuccess()) {
            marketGui.open(player);
        }
        return true;
    }
}

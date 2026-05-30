package com.obm.network.smp.commands;

import com.obm.network.smp.service.EconomyService;
import com.obm.network.smp.service.MarketService;
import com.obm.network.smp.service.MarketService.MarketSaleResult;
import com.obm.network.smp.service.MarketListing;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class MarketCommand implements CommandExecutor {

    private final MarketService marketService;
    private final EconomyService economyService;
    private final int maxListingsPerPlayer;
    private final int minPrice;
    private final int maxPrice;

    public MarketCommand(org.bukkit.plugin.Plugin plugin, MarketService marketService, EconomyService economyService) {
        this.marketService = marketService;
        this.economyService = economyService;
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

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            return handleList(player);
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

    private boolean handleList(Player player) {
        List<MarketListing> listings = marketService.getListings().stream().toList();
        if (listings.isEmpty()) {
            player.sendMessage("§eO mercado está vazio. Use /market sell <preço> para anunciar um item.");
            return true;
        }

        player.sendMessage("§a--- Mercado SMP ---");
        for (MarketListing listing : listings) {
            String sellerName = Bukkit.getOfflinePlayer(listing.getSeller()).getName();
            if (sellerName == null) sellerName = "Desconhecido";
            player.sendMessage("§e" + listing.getId() + "§f - " + listing.getItem().getAmount() + "x " + listing.getItem().getType() + " por §a" + listing.getPrice() + "§f cada (§a" + listing.getTotalValue() + "§f total) - §7" + sellerName);
        }
        return true;
    }

    private boolean handleSell(Player player, String argPrice) {
        try {
            int price = Integer.parseInt(argPrice);
            MarketSaleResult result = marketService.createListing(player, player.getInventory().getItemInMainHand(), price, maxListingsPerPlayer, minPrice, maxPrice);
            player.sendMessage(result.getMessage());
            return true;
        } catch (NumberFormatException ex) {
            player.sendMessage("§cPreço inválido. Use um número inteiro.");
            return true;
        }
    }

    private boolean handleBuy(Player player, String id) {
        MarketSaleResult result = marketService.buyListing(player, id);
        player.sendMessage(result.getMessage());
        return true;
    }

    private boolean handleRemove(Player player, String id) {
        MarketSaleResult result = marketService.removeListing(id, player.getUniqueId());
        player.sendMessage(result.getMessage());
        return true;
    }
}

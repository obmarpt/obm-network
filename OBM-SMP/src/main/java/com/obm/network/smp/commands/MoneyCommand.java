package com.obm.network.smp.commands;

import com.obm.network.smp.permission.SmpPermissions;
import com.obm.network.smp.service.EconomyService;
import com.obm.network.smp.util.SmpRateLimits;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {

    private final EconomyService economyService;

    public MoneyCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        if (args.length == 0) {
            if (SmpPermissions.deny(player, SmpPermissions.MONEY, "Permissão: obm.smp.money")) {
                return true;
            }
            int balance = economyService.getBalance(player.getUniqueId());
            player.sendMessage("§aSeu saldo SMP: §e" + balance + " coins");
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("pay")) {
            if (SmpPermissions.deny(player, SmpPermissions.MONEY_PAY, "Permissão: obm.smp.money.pay")) {
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage("§cJogador não encontrado.");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    player.sendMessage("§cO valor deve ser maior que zero.");
                    return true;
                }

                SmpRateLimits.PayCheckResult rate = SmpRateLimits.checkPay(player, amount);
                if (!rate.allowed()) {
                    player.sendMessage(rate.message());
                    return true;
                }

                if (!economyService.transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
                    player.sendMessage("§cSaldo insuficiente.");
                    return true;
                }

                SmpRateLimits.recordPay(player, amount);
                player.sendMessage("§aVocê pagou §e" + amount + " coins §apara §f" + target.getName() + "§a.");
                target.sendMessage("§aVocê recebeu §e" + amount + " coins §ade §f" + player.getName() + "§a.");
            } catch (NumberFormatException ex) {
                player.sendMessage("§cValor inválido. Use um número inteiro.");
            }
            return true;
        }

        player.sendMessage("§eUso: /money [pay <player> <amount>]");
        return true;
    }
}

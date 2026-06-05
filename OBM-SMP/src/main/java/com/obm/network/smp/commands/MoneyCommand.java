package com.obm.network.smp.commands;

import com.obm.network.core.ui.PlayerUx;
import com.obm.network.smp.permission.SmpPermissions;
import com.obm.network.smp.service.EconomyService;
import com.obm.network.smp.util.SmpRateLimits;
import org.bukkit.Bukkit;
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
        if (!PlayerUx.requirePlayer(sender)) {
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            if (SmpPermissions.deny(player, SmpPermissions.MONEY, "Precisas de: obm.smp.money")) {
                return true;
            }
            int balance = economyService.getBalance(player.getUniqueId());
            PlayerUx.success(player, "Saldo SMP: §e" + economyService.format(balance));
            PlayerUx.actionBar(player, "§7Coins: §e" + economyService.format(balance));
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("pay")) {
            if (SmpPermissions.deny(player, SmpPermissions.MONEY_PAY, "Precisas de: obm.smp.money.pay")) {
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                PlayerUx.error(player, "Jogador não encontrado.");
                PlayerUx.errorSound(player);
                return true;
            }

            try {
                int amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    PlayerUx.error(player, "O valor deve ser maior que zero.");
                    return true;
                }

                SmpRateLimits.PayCheckResult rate = SmpRateLimits.checkPay(player, amount);
                if (!rate.allowed()) {
                    player.sendMessage(rate.message());
                    return true;
                }

                if (!economyService.transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
                    PlayerUx.error(player, "Não tens dinheiro suficiente.");
                    PlayerUx.errorSound(player);
                    return true;
                }

                SmpRateLimits.recordPay(player, amount);
                String formatted = economyService.format(amount);
                PlayerUx.success(player, "Pagaste §e" + formatted + " §apara §f" + target.getName());
                PlayerUx.success(target, "Recebeste §e" + formatted + " §ade §f" + player.getName());
                PlayerUx.notifyMoneyGain(target, amount, formatted);
                PlayerUx.successSound(player);
            } catch (NumberFormatException ex) {
                PlayerUx.error(player, "Valor inválido — usa um número inteiro.");
            }
            return true;
        }

        PlayerUx.usage(player, "/money §7| §f/money pay <jogador> <valor>");
        return true;
    }
}

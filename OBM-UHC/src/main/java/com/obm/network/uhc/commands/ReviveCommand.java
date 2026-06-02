package com.obm.network.uhc.commands;

import com.obm.network.uhc.service.UHCReviveService;
import com.obm.network.uhc.service.UHCReviveService.ReviveType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReviveCommand implements CommandExecutor {

    private static final String PERMISSION = "uhc.revive";

    private final UHCReviveService reviveService;

    public ReviveCommand(UHCReviveService reviveService) {
        this.reviveService = reviveService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("Sem permissão.", NamedTextColor.RED));
            return true;
        }

        if (label.equalsIgnoreCase("revivegeral")) {
            int count = reviveService.reviveAllEliminated();
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(Component.text(
                        "Ocorreu um Revive Geral! Quando entrares no UHC estarás vivo.",
                        NamedTextColor.GREEN));
            }
            sender.sendMessage(Component.text(
                    "Revive Geral aplicado em " + count + " jogadores na base de dados.",
                    NamedTextColor.GREEN));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Uso correto: /" + label + " <jogador>", NamedTextColor.RED));
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Component.text("Esse jogador nunca entrou no servidor.", NamedTextColor.RED));
            return true;
        }

        if (label.equalsIgnoreCase("revivetotal")) {
            reviveService.scheduleRevive(target.getUniqueId(), ReviveType.TOTAL);
            sender.sendMessage(Component.text(
                    "Revive Total AGENDADO para " + target.getName() + ". Ele será revivido ao entrar no UHC.",
                    NamedTextColor.GREEN));
            notifyTarget(target, "Foste revivido com um REVIVE TOTAL! Entra no UHC para voltar ao jogo.");
            return true;
        }

        if (label.equalsIgnoreCase("revive")) {
            reviveService.scheduleRevive(target.getUniqueId(), ReviveType.NORMAL);
            sender.sendMessage(Component.text(
                    "Revive Simples aplicado em " + target.getName() + ".",
                    NamedTextColor.GREEN));
            notifyTarget(target, "Foste revivido! Entra no UHC para voltar ao jogo.");
            return true;
        }

        return true;
    }

    private void notifyTarget(OfflinePlayer target, String message) {
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(Component.text(message, NamedTextColor.GREEN));
        }
    }
}

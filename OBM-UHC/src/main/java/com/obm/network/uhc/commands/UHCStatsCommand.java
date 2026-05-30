package com.obm.network.uhc.commands;

import com.obm.network.uhc.gui.UHCStatsMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UHCStatsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 1. Verifica se quem executou foi um jogador
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Apenas jogadores podem usar este comando.", NamedTextColor.RED));
            return true;
        }

        // 2. Abre a GUI delegando a responsabilidade para a classe correta na pasta gui
        UHCStatsMenu.open(player);
        return true;
    }
}
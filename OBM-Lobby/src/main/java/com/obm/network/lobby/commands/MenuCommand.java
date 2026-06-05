package com.obm.network.lobby.commands;

import com.obm.network.core.ui.PlayerUx;
import com.obm.network.lobby.gui.MainMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MenuCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!PlayerUx.requirePlayer(sender)) {
            return true;
        }
        Player player = (Player) sender;
        if (!PlayerUx.requirePermission(player, "obm.menu")) {
            PlayerUx.errorSound(player);
            return true;
        }
        PlayerUx.openGuiFeedback(player, "Menu de Modos");
        player.openInventory(MainMenu.create(player));
        return true;
    }
}

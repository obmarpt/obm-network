package com.obm.network.lobby.listener;

import com.obm.network.lobby.gui.MainMenu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class LobbyItemListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        Player player = e.getPlayer();
        ItemStack item = e.getItem();

        if (item == null) return;

        // ✅ DETETAR NETHER STAR
        if (item.getType() != Material.NETHER_STAR) return;

        // ✅ cancelar comportamento (segurança)
        e.setCancelled(true);

        // ✅ abrir menu
        player.openInventory(MainMenu.create(player));
    }
}
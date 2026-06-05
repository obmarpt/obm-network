package com.obm.network.lobby.listener;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.battlepass.BattlePassGui;
import com.obm.network.core.location.LobbySpawnService;
import com.obm.network.lobby.LobbyWorldService;
import com.obm.network.lobby.gui.MainMenu;
import com.obm.network.lobby.gui.ProfileMenu;
import com.obm.network.lobby.items.LobbyItemsService;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class LobbyItemListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!LobbyWorldService.isInLobby(player)) {
            return;
        }

        ItemStack item = event.getItem();
        if (!LobbyItemsService.isLobbyItem(item)) {
            return;
        }

        event.setCancelled(true);
        LobbyItemsService.LobbyItem type = LobbyItemsService.typeOf(item);
        if (type == null) {
            return;
        }

        playClick(player);

        switch (type) {
            case NAVIGATOR -> player.openInventory(MainMenu.create(player));
            case BATTLE_PASS -> openBattlePass(player);
            case PROFILE -> player.openInventory(ProfileMenu.create(player));
            case BACK -> {
                LobbySpawnService.teleportToLobby(player);
                player.sendMessage("§7Regressaste ao spawn do lobby.");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 1.4f);
            }
        }
    }

    private void openBattlePass(Player player) {
        OBMCorePlugin core = OBMCorePlugin.get();
        if (core == null) {
            player.sendMessage("§cBattle Pass indisponível.");
            return;
        }
        BattlePassGui gui = core.getBattlePassGui();
        if (gui == null) {
            player.sendMessage("§cBattle Pass indisponível.");
            return;
        }
        gui.open(player);
    }

    private void playClick(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }
}

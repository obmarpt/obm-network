package com.obm.network.smp.listener;

import com.obm.network.core.ui.PlayerUx;
import com.obm.network.smp.progression.RankGui;
import com.obm.network.smp.progression.RankService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class RankGuiListener implements Listener {

    private final RankGui rankGui;
    private final RankService rankService;

    public RankGuiListener(RankGui rankGui, RankService rankService) {
        this.rankGui = rankGui;
        this.rankService = rankService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!rankGui.isRankTitle(title)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot == RankGui.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        String rankId = rankGui.extractRankId(clicked);
        if (rankId == null) {
            return;
        }

        if (!rankGui.isNextRank(player, rankId)) {
            if (rankService.getCatalog().getNextRank(rankService.getRank(player.getUniqueId()).id()).isEmpty()) {
                PlayerUx.error(player, "Já atingiste o rank máximo.");
            } else {
                PlayerUx.error(player, "Só podes comprar o próximo rank na sequência.");
            }
            PlayerUx.errorSound(player);
            return;
        }

        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
        var result = rankService.purchaseNextRank(player, false);
        if (result.success()) {
            player.closeInventory();
            rankGui.celebrate(player, rankService.getRank(player.getUniqueId()));
        } else {
            PlayerUx.error(player, result.message());
            PlayerUx.errorSound(player);
            rankGui.open(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (rankGui.isRankTitle(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }
}

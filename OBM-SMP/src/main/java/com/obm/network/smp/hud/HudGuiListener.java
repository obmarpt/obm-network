package com.obm.network.smp.hud;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.hud.HudFeature;
import com.obm.network.core.hud.HudPreferencesService;
import com.obm.network.core.ui.PlayerUx;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class HudGuiListener implements Listener {

    private final HudGui hudGui;

    public HudGuiListener(HudGui hudGui) {
        this.hudGui = hudGui;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!hudGui.isHudTitle(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        HudFeature feature = hudGui.featureForSlot(event.getRawSlot());
        if (feature == null) {
            return;
        }

        HudPreferencesService service = preferences();
        if (service == null) {
            return;
        }

        service.toggle(player, feature);
        PlayerUx.confirmSound(player);
        player.sendMessage("§aHUD atualizado!");
        hudGui.refresh(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (hudGui.isHudTitle(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    private static HudPreferencesService preferences() {
        OBMCorePlugin core = OBMCorePlugin.get();
        return core == null ? null : core.getHudPreferencesService();
    }
}

package com.obm.network.tierspace.listener;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.location.RankedHubService;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.tierspace.match.MatchService;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.ui.TierGuiMenu;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * NPCs TierSpace / Ranked: no hub abre menu; com "fila"/"queue" no nome entra na queue sword.
 */
public final class TierSpaceNpcListener implements Listener {

    private final TierGuiMenu tierGuiMenu;
    private final MatchService matchService;
    private final WorldModeService worldModeService;

    public TierSpaceNpcListener(TierGuiMenu tierGuiMenu, MatchService matchService) {
        this.tierGuiMenu = tierGuiMenu;
        this.matchService = matchService;
        OBMCorePlugin core = OBMCorePlugin.get();
        this.worldModeService = core != null ? core.getWorldModeService() : null;
    }

    @EventHandler
    public void onNpcClick(NPCRightClickEvent event) {
        String name = event.getNPC().getName();
        if (!isTierSpaceNpc(name)) {
            return;
        }

        Player player = event.getClicker();
        if (player.isDead() || player.getHealth() <= 0) {
            return;
        }

        String lower = name.toLowerCase();
        if (lower.contains("queue") || lower.contains("fila")) {
            player.closeInventory();
            matchService.joinQueue(player, resolveQueueMode(lower));
            return;
        }

        if (worldModeService != null && worldModeService.isRanked(player.getWorld().getName())) {
            player.openInventory(tierGuiMenu.create(player));
            return;
        }

        if (RankedHubService.teleport(player)) {
            player.sendMessage("§bTierSpace §8| §7Hub ranked. Usa a §fbússola §7ou este NPC outra vez.");
        }
    }

    private GameModeId resolveQueueMode(String npcNameLower) {
        if (npcNameLower.contains("nodebuff") || npcNameLower.contains("pot")) {
            return GameModeId.NODEBUFF;
        }
        if (npcNameLower.contains("uhc")) {
            return GameModeId.UHC;
        }
        return GameModeId.SWORD;
    }

    private boolean isTierSpaceNpc(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.contains("tierspace") || lower.contains("tier space") || lower.contains("ranked");
    }
}

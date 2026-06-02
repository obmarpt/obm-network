package com.obm.network.tierspace.progression;

import com.obm.network.core.tier.TierRank;
import com.obm.network.core.tier.TierRankUtil;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.storage.TierSpaceStore;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlacementService {

    private final TierSpaceStore store;
    private final int placementGames;
    private final int placementKFactor;

    public PlacementService(TierSpaceStore store, int placementGames, int placementKFactor) {
        this.store = store;
        this.placementGames = placementGames;
        this.placementKFactor = placementKFactor;
    }

    public boolean isInPlacement(UUID uuid, GameModeId mode) {
        store.ensureInitialized(uuid, mode);
        if (store.isPlaced(uuid, mode)) {
            return false;
        }
        return store.getPlacementMatches(uuid, mode) < placementGames;
    }

    public int getPlacementProgress(UUID uuid, GameModeId mode) {
        return Math.min(placementGames, store.getPlacementMatches(uuid, mode));
    }

    public String getPlacementLabel(UUID uuid, GameModeId mode) {
        return "Placement: " + getPlacementProgress(uuid, mode) + "/" + placementGames;
    }

    public int getPlacementKFactor() {
        return placementKFactor;
    }

    /**
     * @return true if placement was just completed (10th game)
     */
    public boolean recordMatch(UUID uuid, GameModeId mode) {
        if (store.isPlaced(uuid, mode)) {
            return false;
        }

        int next = store.getPlacementMatches(uuid, mode) + 1;
        store.setPlacementMatches(uuid, mode, next);

        if (next >= placementGames) {
            store.setPlaced(uuid, mode, true);
            return true;
        }
        return false;
    }

    public void sendRankReveal(Player player, GameModeId mode) {
        int rating = store.getRating(player.getUniqueId(), mode);
        TierRank rank = TierRankUtil.fromRating(rating);
        player.sendTitle("§6§lRANK REVEALED", rank.displayName(), 10, 70, 20);
        player.sendMessage("§6§lRANK REVEALED §7→ " + rank.displayName() + " §7(§f" + rating + " rating§7)");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }
}

package com.obm.network.tierspace.command;

import com.obm.network.core.tier.TierRankUtil;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.progression.DailyQuestService;
import com.obm.network.tierspace.progression.PlacementService;
import com.obm.network.tierspace.season.TierSeasonManager;
import com.obm.network.tierspace.storage.TierSpaceStore;
import com.obm.network.tierspace.ui.TierGuiMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TierSpaceCommand implements CommandExecutor {

    private final TierSpaceStore store;
    private final TierGuiMenu tierGuiMenu;
    private final ModeRegistry modeRegistry;
    private final TierSeasonManager seasonManager;

    public TierSpaceCommand(TierSpaceStore store,
                            TierGuiMenu tierGuiMenu,
                            ModeRegistry modeRegistry,
                            TierSeasonManager seasonManager) {
        this.store = store;
        this.tierGuiMenu = tierGuiMenu;
        this.modeRegistry = modeRegistry;
        this.seasonManager = seasonManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            player.openInventory(tierGuiMenu.create(player));
            return true;
        }

        if (args[0].equalsIgnoreCase("stats")) {
            showStats(player, GameModeId.from(args.length > 1 ? args[1] : "sword").orElse(GameModeId.SWORD));
            return true;
        }

        GameModeId mode = GameModeId.from(args[0]).orElse(GameModeId.SWORD);
        showStats(player, mode);
        return true;
    }

    private void showStats(Player player, GameModeId mode) {
        store.ensureInitialized(player.getUniqueId(), mode);
        seasonManager.ensurePlayerSeason(player);

        int rating = store.getRating(player.getUniqueId(), mode);
        int wins = store.getWins(player.getUniqueId(), mode);
        int losses = store.getLosses(player.getUniqueId(), mode);
        int streak = store.getStreak(player.getUniqueId(), mode);

        PlacementService placement = com.obm.network.tierspace.TierSpacePlugin.get().getPlacementService();
        boolean inPlacement = placement != null && placement.isInPlacement(player.getUniqueId(), mode);

        player.sendMessage("§b§lTierSpace");
        player.sendMessage(seasonManager.getSeasonDisplayLine());
        player.sendMessage("§7Modo: §f" + modeRegistry.displayName(mode));
        if (inPlacement) {
            player.sendMessage("§e" + placement.getPlacementLabel(player.getUniqueId(), mode));
            player.sendMessage("§7Rating: §f" + rating + " §7(oculto até placement)");
        } else {
            player.sendMessage("§7Rank: " + TierRankUtil.fromRating(rating).displayName());
            player.sendMessage("§7Rating: §f" + rating);
            player.sendMessage("§7" + TierRankUtil.formatNextRankLine(rating));
            player.sendMessage("§7Progress: §f" + TierRankUtil.formatProgress(rating));
        }
        player.sendMessage("§7W/L: §a" + wins + " §7/ §c" + losses + " §7| Streak: §e" + streak);

        DailyQuestService quests = com.obm.network.tierspace.TierSpacePlugin.get().getDailyQuestService();
        if (quests != null) {
            player.sendMessage(quests.getProgressLine(player.getUniqueId(), mode));
        }

        player.sendMessage("§7Menu: §f/tierspace menu §7| Fila: §f/queue " + mode.id());
    }
}

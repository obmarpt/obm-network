package com.obm.network.tierspace.command;

import com.obm.network.core.tier.CompetitiveTier;
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
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TierSpaceCommand implements CommandExecutor, TabCompleter {

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

        if (args.length >= 1 && args[0].equalsIgnoreCase("season")) {
            return handleSeason(sender, args);
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
        int kills = store.getKills(player.getUniqueId(), mode);
        int deaths = store.getDeaths(player.getUniqueId(), mode);

        PlacementService placement = com.obm.network.tierspace.TierSpacePlugin.get().getPlacementService();
        boolean inPlacement = placement != null && placement.isInPlacement(player.getUniqueId(), mode);

        player.sendMessage("§b§lTierSpace");
        player.sendMessage(seasonManager.getSeasonDisplayLine());
        player.sendMessage("§7Modo: §f" + modeRegistry.displayName(mode));
        if (inPlacement) {
            player.sendMessage("§e" + placement.getPlacementLabel(player.getUniqueId(), mode));
            player.sendMessage("§7Rating: §f" + rating + " §7(oculto até placement)");
        } else {
            CompetitiveTier tier = CompetitiveTier.getTierFromRating(rating);
            player.sendMessage("§7Tier: " + tier.displayName());
            player.sendMessage("§7Elo: §f" + rating);
            player.sendMessage("§7" + TierRankUtil.formatNextRankLine(rating));
            player.sendMessage("§7Progresso: §f" + TierRankUtil.formatProgress(rating));
        }
        player.sendMessage("§7W/L: §a" + wins + " §7/ §c" + losses + " §7| Streak: §e" + streak);
        player.sendMessage("§7K/D: §f" + kills + "§7/§f" + deaths + " §8(§f" + store.formatKd(player.getUniqueId(), mode) + "§8)");

        DailyQuestService quests = com.obm.network.tierspace.TierSpacePlugin.get().getDailyQuestService();
        if (quests != null) {
            player.sendMessage(quests.getProgressLine(player.getUniqueId(), mode));
        }

        player.sendMessage("§7Menu: §f/tierspace menu §7| Fila: §f/queue " + mode.id());
    }

    private boolean handleSeason(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tierspace.admin.season")) {
            sender.sendMessage("§cSem permissão.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§e/tierspace season advance §7— nova temporada + reset suave");
            sender.sendMessage("§e/tierspace season set <n> §7— corrigir número (sem reset)");
            sender.sendMessage("§7Atual: §f" + seasonManager.getSeasonDisplayLine());
            return true;
        }
        if (args[1].equalsIgnoreCase("advance") || args[1].equalsIgnoreCase("start")) {
            seasonManager.startNewSeason(true, sender);
            return true;
        }
        if (args[1].equalsIgnoreCase("set") && args.length >= 3) {
            try {
                int n = Integer.parseInt(args[2]);
                seasonManager.setSeasonNumberManually(n, sender);
            } catch (NumberFormatException ex) {
                sender.sendMessage("§cNúmero inválido.");
            }
            return true;
        }
        sender.sendMessage("§cUso: /tierspace season <advance|set <n>>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("tierspace.admin.season")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("season", "menu", "stats"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("season")) {
            return filter(List.of("advance", "set"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}

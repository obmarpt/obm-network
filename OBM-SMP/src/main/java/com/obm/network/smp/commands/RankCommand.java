package com.obm.network.smp.commands;

import com.obm.network.smp.progression.RankDefinition;
import com.obm.network.smp.progression.RankService;
import com.obm.network.smp.progression.LevelService;
import com.obm.network.smp.progression.PlayerProgressionStore;
import com.obm.network.smp.progression.RankCatalog;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankCommand implements CommandExecutor {

    private final RankService rankService;
    private final LevelService levelService;
    private final RankCatalog rankCatalog;
    private final PlayerProgressionStore store;

    public RankCommand(RankService rankService, LevelService levelService,
                       RankCatalog rankCatalog, PlayerProgressionStore store) {
        this.rankService = rankService;
        this.levelService = levelService;
        this.rankCatalog = rankCatalog;
        this.store = store;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("buy")) {
            var result = rankService.purchaseNextRank(player);
            player.sendMessage((result.success() ? "§a" : "§c") + result.message());
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§6--- Ranks SMP ---");
            for (RankDefinition rank : rankCatalog.getRanks()) {
                player.sendMessage("§e" + rank.displayName() + " §7- §f" + rank.cost() + " coins"
                        + " §8| +venda " + pct(rank.sellBoost())
                        + " +kill " + pct(rank.killBoost())
                        + " -loja " + pct(rank.shopDiscount()));
            }
            return true;
        }

        var rank = rankService.getRank(player.getUniqueId());
        int level = levelService.getLevel(player.getUniqueId());
        int xp = levelService.getXp(player.getUniqueId());
        int required = levelService.getXpRequired(level);

        player.sendMessage("§6--- Progressão ---");
        player.sendMessage("§7Rank: §e" + rank.displayName());
        player.sendMessage("§7Nível: §e" + level + " §8(§f" + xp + "§7/§f" + required + " XP§8)");
        player.sendMessage("§7Bónus: §a+" + pct(rank.sellBoost()) + " venda §7| §c+" + pct(rank.killBoost())
                + " kills §7| §b-" + pct(rank.shopDiscount()) + " loja");
        rankCatalog.getNextRank(rank.id()).ifPresentOrElse(
                next -> player.sendMessage("§7Próximo rank: §e" + next.displayName() + " §7(§f" + next.cost() + " coins§7) §8- §a/rank buy"),
                () -> player.sendMessage("§7Rank máximo alcançado.")
        );
        return true;
    }

    private String pct(double value) {
        return String.format("%.0f%%", value * 100);
    }
}

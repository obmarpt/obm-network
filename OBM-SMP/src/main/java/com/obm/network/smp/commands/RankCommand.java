package com.obm.network.smp.commands;

import com.obm.network.smp.permission.SmpPermissions;
import com.obm.network.smp.progression.RankDefinition;
import com.obm.network.smp.progression.RankGui;
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
    private final RankGui rankGui;

    public RankCommand(RankService rankService, LevelService levelService,
                       RankCatalog rankCatalog, PlayerProgressionStore store, RankGui rankGui) {
        this.rankService = rankService;
        this.levelService = levelService;
        this.rankCatalog = rankCatalog;
        this.store = store;
        this.rankGui = rankGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }
        if (SmpPermissions.deny(player, SmpPermissions.RANK, "Permissão: obm.smp.rank")) {
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("up"))) {
            var result = rankService.purchaseNextRank(player);
            player.sendMessage((result.success() ? "§a" : "§c") + result.message());
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            rankGui.open(player);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            showInfo(player);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§6§l--- Ranks Rush SMP ---");
            for (RankDefinition rank : rankCatalog.getRanks()) {
                String cost = rank.cost() <= 0 ? "§7Inicial" : "§f" + rank.cost() + " Money";
                player.sendMessage(rank.chatPrefix() + " §e" + rank.displayName()
                        + " §8| " + cost
                        + " §8| §a+" + pct(rank.moneyBoost()) + " money"
                        + " §8| §e" + rank.extraHomes() + " homes");
            }
            player.sendMessage("§7Usa §f/rankup §7para subir de rank.");
            return true;
        }

        rankGui.open(player);
        return true;
    }

    private void showInfo(Player player) {
        var rank = rankService.getRank(player.getUniqueId());
        int level = levelService.getLevel(player.getUniqueId());
        int xp = levelService.getXp(player.getUniqueId());
        int required = levelService.getXpRequired(level);

        player.sendMessage("§6--- Progressão ---");
        player.sendMessage("§7Rank: §e" + rank.displayName());
        player.sendMessage("§aSMP Level: §f" + level + " §8(§f" + xp + "§7/§f" + required + " XP§8)");
        player.sendMessage("§7Bónus: §a+" + pct(rank.moneyBoost()) + " money §7| §e" + rank.extraHomes() + " homes"
                + (rank.cooldownReduction() > 0 ? " §7| §b-" + pct(rank.cooldownReduction()) + " CD" : ""));
        rankCatalog.getNextRank(rank.id()).ifPresentOrElse(
                next -> player.sendMessage("§7Próximo: §e" + next.displayName()
                        + " §7(§f" + next.cost() + " Money§7) §8— §a/rank §7ou §a/rankup"),
                () -> player.sendMessage("§7Rank máximo alcançado.")
        );
    }

    private String pct(double value) {
        return String.format("%.0f%%", value * 100);
    }
}

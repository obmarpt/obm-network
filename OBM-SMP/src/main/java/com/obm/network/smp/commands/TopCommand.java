package com.obm.network.smp.commands;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.leaderboard.LeaderboardService;
import com.obm.network.core.storage.PlayerStatsKeys;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TopCommand implements CommandExecutor, TabCompleter {

    private static final Map<String, String> STATS = new LinkedHashMap<>();

    static {
        STATS.put("kills", PlayerStatsKeys.KILLS_SMP);
        STATS.put("playtime", PlayerStatsKeys.PLAYTIME_SMP);
        STATS.put("blocks", PlayerStatsKeys.BLOCKS_BROKEN_SMP);
        STATS.put("coins", PlayerStatsKeys.SMP_BALANCE);
        STATS.put("earned", PlayerStatsKeys.SMP_TOTAL_EARNED);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OBMCorePlugin core = OBMCorePlugin.get();
        if (core == null) {
            sender.sendMessage("§cCore indisponível.");
            return true;
        }
        LeaderboardService leaderboard = core.getLeaderboardService();
        if (leaderboard == null) {
            sender.sendMessage("§cLeaderboard indisponível.");
            return true;
        }

        String statKey = PlayerStatsKeys.KILLS_SMP;
        int limit = 10;
        if (args.length >= 1) {
            String arg = args[0].toLowerCase(Locale.ROOT);
            statKey = STATS.getOrDefault(arg, statKey);
        }
        if (args.length >= 2) {
            try {
                limit = Math.min(20, Math.max(1, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
            }
        }

        List<UUID> top = leaderboard.getTop(statKey, limit);
        String labelStat = labelFor(statKey);
        sender.sendMessage("§6§lTOP RushSMP §7— §f" + labelStat);
        if (top.isEmpty()) {
            sender.sendMessage("§7(sem dados)");
            return true;
        }
        for (int i = 0; i < top.size(); i++) {
            UUID uuid = top.get(i);
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            int value = leaderboard.getStat(uuid, statKey);
            sender.sendMessage("§e#" + (i + 1) + " §f" + (name != null ? name : uuid.toString().substring(0, 8))
                    + " §7— §f" + formatValue(statKey, value));
        }
        if (!(sender instanceof Player)) {
            return true;
        }
        return true;
    }

    private static String labelFor(String statKey) {
        return switch (statKey) {
            case "playtime_smp" -> "Playtime";
            case "blocks_broken_smp" -> "Blocos partidos";
            case "smp_balance" -> "Coins";
            case "smp_total_earned" -> "Coins ganhos (total)";
            default -> "Kills";
        };
    }

    private static String formatValue(String statKey, int value) {
        if ("playtime_smp".equals(statKey)) {
            int hours = value / 3600;
            int mins = (value % 3600) / 60;
            return hours + "h " + mins + "m";
        }
        return String.valueOf(value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String key : STATS.keySet()) {
                if (key.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(key);
                }
            }
        }
        return out;
    }
}

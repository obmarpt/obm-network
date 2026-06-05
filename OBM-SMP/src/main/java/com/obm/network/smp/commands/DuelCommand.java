package com.obm.network.smp.commands;

import com.obm.network.core.ui.PlayerUx;
import com.obm.network.smp.duel.DuelService;
import com.obm.network.smp.permission.SmpPermissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DuelCommand implements CommandExecutor, TabCompleter {

    private final DuelService duelService;

    public DuelCommand(DuelService duelService) {
        this.duelService = duelService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!PlayerUx.requirePlayer(sender)) {
            return true;
        }
        Player player = (Player) sender;

        if (SmpPermissions.deny(player, SmpPermissions.DUEL, "Permissão: obm.duel.use")) {
            PlayerUx.errorSound(player);
            return true;
        }
        if (!duelService.isEnabled()) {
            PlayerUx.error(player, "Duelos estão desativados.");
            return true;
        }

        if (args.length == 0) {
            PlayerUx.usageLines(player,
                    "§e/duel <jogador> §8— §7desafiar para BO3",
                    "§e/duel accept §8— §7aceitar desafio",
                    "§e/duel deny §8— §7recusar desafio");
            PlayerUx.hint(player, "Mais info: §f/help duel");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("accept".equals(sub)) {
            duelService.accept(player);
            return true;
        }
        if ("deny".equals(sub) || "decline".equals(sub)) {
            duelService.deny(player);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            PlayerUx.error(player, "Jogador não encontrado.");
            PlayerUx.errorSound(player);
            return true;
        }
        duelService.challenge(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String opt : List.of("accept", "deny")) {
                if (opt.startsWith(prefix)) {
                    out.add(opt);
                }
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(online.getName());
                }
            }
            return out;
        }
        return List.of();
    }
}

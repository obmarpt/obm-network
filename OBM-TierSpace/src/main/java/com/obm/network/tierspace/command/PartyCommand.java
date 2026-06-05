package com.obm.network.tierspace.command;

import com.obm.network.tierspace.hub.TierSpaceHub;
import com.obm.network.tierspace.party.PartyService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PartyCommand implements CommandExecutor, TabCompleter {

    private final PartyService partyService;

    public PartyCommand(PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores.");
            return true;
        }
        if (!TierSpaceHub.isInTierSpaceHub(player)) {
            player.sendMessage("§cSó podes usar este comando no TierSpace.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§d/party create §7| §dinvite <jogador> §7| §daccept §7| §dleave");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> {
                partyService.create(player);
                player.sendMessage("§dParty §8| §aParty criada.");
            }
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage("§c/party invite <jogador>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§cJogador offline.");
                    return true;
                }
                partyService.invite(player, target);
            }
            case "accept" -> partyService.accept(player);
            case "leave" -> partyService.leave(player.getUniqueId());
            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage("§c/party kick <jogador>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§cJogador offline.");
                    return true;
                }
                partyService.kick(player, target.getUniqueId());
            }
            default -> player.sendMessage("§cSubcomando inválido.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "invite", "accept", "leave", "kick");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return names;
        }
        return List.of();
    }
}

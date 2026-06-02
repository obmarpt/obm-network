package com.obm.network.tierspace.command;

import com.obm.network.tierspace.match.MatchService;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.queue.QueueFeedbackService;
import com.obm.network.tierspace.queue.QueueService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class QueueCommand implements CommandExecutor, TabCompleter {

    private final MatchService matchService;
    private final QueueService queueService;
    private final QueueFeedbackService queueFeedbackService;
    private final ModeRegistry modeRegistry;

    public QueueCommand(MatchService matchService,
                        QueueService queueService,
                        QueueFeedbackService queueFeedbackService,
                        ModeRegistry modeRegistry) {
        this.matchService = matchService;
        this.queueService = queueService;
        this.queueFeedbackService = queueFeedbackService;
        this.modeRegistry = modeRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§b/queue <modo> §7| §b/queue leave §7| §b/queue status");
            player.sendMessage("§7Modos: " + String.join(", ",
                    modeRegistry.enabledModes().stream().map(GameModeId::id).toList()));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("leave")) {
            UUID uuid = player.getUniqueId();
            if (queueService.leave(uuid)) {
                queueFeedbackService.stopTracking(uuid);
                player.sendMessage("§7Saíste da fila TierSpace.");
            } else {
                player.sendMessage("§cNão estás em fila.");
            }
            return true;
        }

        if (sub.equals("status")) {
            queueService.getEntry(player.getUniqueId()).ifPresentOrElse(entry -> {
                player.sendMessage("§bFila: §f" + modeRegistry.displayName(entry.mode()));
                player.sendMessage("§7Rating: §f" + entry.rating());
            }, () -> player.sendMessage("§7Não estás em fila."));
            return true;
        }

        GameModeId mode = GameModeId.from(sub).orElse(null);
        if (mode == null) {
            player.sendMessage("§cModo inválido.");
            return true;
        }
        if (!modeRegistry.isEnabled(mode)) {
            player.sendMessage("§cEste modo não está disponível.");
            return true;
        }

        matchService.joinQueue(player, mode);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("leave");
            options.add("status");
            for (GameModeId mode : modeRegistry.enabledModes()) {
                options.add(mode.id());
            }
            return options;
        }
        return List.of();
    }
}

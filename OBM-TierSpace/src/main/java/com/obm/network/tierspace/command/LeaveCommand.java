package com.obm.network.tierspace.command;

import com.obm.network.tierspace.hub.TierSpaceHub;
import com.obm.network.tierspace.queue.QueueFeedbackService;
import com.obm.network.tierspace.queue.QueueService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class LeaveCommand implements CommandExecutor {

    private final QueueService queueService;
    private final QueueFeedbackService queueFeedbackService;

    public LeaveCommand(QueueService queueService, QueueFeedbackService queueFeedbackService) {
        this.queueService = queueService;
        this.queueFeedbackService = queueFeedbackService;
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
        UUID uuid = player.getUniqueId();
        if (queueService.leave(uuid)) {
            queueFeedbackService.stopTracking(uuid);
            player.sendMessage("§dTierSpace §8| §7Saíste da fila.");
        } else {
            player.sendMessage("§cNão estás em fila.");
        }
        return true;
    }
}

package com.obm.network.lobby.commands;

import com.obm.network.core.combat.CombatLogService;
import com.obm.network.core.combat.CooldownService;
import com.obm.network.core.location.LobbySpawnService;
import com.obm.network.core.player.PlayerStateReset;
import com.obm.network.lobby.OBMLobbyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommand implements CommandExecutor {

    private final OBMLobbyPlugin plugin;

    public LobbyCommand(OBMLobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }

        boolean force = plugin.getConfig().getBoolean("lobby.command.force-always", true);

        if (!force) {
            if (CombatLogService.isInCombat(player)) {
                player.sendMessage("§cNão podes usar /lobby enquanto estás em combate!");
                return true;
            }
            if (!CooldownService.canUseLobby(player)) {
                long rem = CooldownService.getRemainingSeconds(player);
                player.sendMessage("§cAinda não podes voltar ao lobby. Aguarda " + rem + " segundos.");
                return true;
            }
        }

        CombatLogService.clearCombat(player);
        CooldownService.clear(player);
        PlayerStateReset.clearTransientState(player);

        if (!LobbySpawnService.teleportToLobby(player)) {
            player.sendMessage("§cLobby ainda não configurado.");
            plugin.getLogger().warning("Mundo do lobby indisponível (ver OBM-Core config lobby.world/spawn).");
            return true;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                PlayerStateReset.resyncVisual(
                        com.obm.network.core.OBMCorePlugin.get(), player, true);
            }
        }, 2L);

        player.sendMessage("§aFoste teleportado para o lobby.");
        return true;
    }
}

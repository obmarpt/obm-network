package com.obm.network.lobby.commands;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.combat.CombatLogService;
import com.obm.network.core.combat.CooldownService;
import com.obm.network.core.location.LobbySpawnService;
import com.obm.network.core.player.PlayerStateReset;
import com.obm.network.lobby.LobbyWorldService;
import com.obm.network.lobby.OBMLobbyPlugin;
import com.obm.network.lobby.items.LobbyItemsService;
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
        if (plugin.isCommandDebug()) {
            plugin.getLogger().info("[lobby-debug] executado por " + sender.getName()
                    + " label=" + label + " args=" + args.length);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.");
            return true;
        }

        if (!sender.hasPermission("obm.lobby")) {
            sender.sendMessage("§cNão tens permissão para usar este comando.");
            if (plugin.isCommandDebug()) {
                plugin.getLogger().warning("[lobby-debug] sem permissão obm.lobby: " + player.getName());
            }
            return true;
        }

        boolean force = plugin.getConfig().getBoolean("lobby.command.force-always", true);
        boolean bypass = player.hasPermission("obm.lobby.bypass");

        if (!bypass && CombatLogService.isTaggedInSmpCombat(player)) {
            player.sendMessage("§cNão podes usar /lobby enquanto estás em combate!");
            if (plugin.isCommandDebug()) {
                plugin.getLogger().info("[lobby-debug] bloqueado (combat tag): " + player.getName());
            }
            return true;
        }

        if (!force && !bypass) {
            if (!CooldownService.canUseLobby(player)) {
                long rem = CooldownService.getRemainingSeconds(player);
                player.sendMessage("§cAinda não podes voltar ao lobby. Aguarda " + rem + " segundos.");
                if (plugin.isCommandDebug()) {
                    plugin.getLogger().info("[lobby-debug] bloqueado (cooldown " + rem + "s): " + player.getName());
                }
                return true;
            }
        }

        PlayerStateReset.clearTransientState(player);

        if (!LobbySpawnService.teleportToLobby(player)) {
            player.sendMessage("§cLobby ainda não configurado.");
            plugin.getLogger().warning("[lobby] mundo/spawn indisponível para " + player.getName()
                    + " — verifica OBM-Core config lobby.world/spawn");
            return true;
        }

        CooldownService.clear(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            OBMCorePlugin core = OBMCorePlugin.get();
            if (core != null) {
                PlayerStateReset.resyncVisual(core, player, true);
            }
            if (LobbyWorldService.isInLobby(player)) {
                LobbyItemsService.equip(player);
            }
        }, 3L);

        player.sendMessage("§aFoste teleportado para o lobby.");
        if (plugin.isCommandDebug()) {
            plugin.getLogger().info("[lobby-debug] teleport OK: " + player.getName());
        }
        return true;
    }
}

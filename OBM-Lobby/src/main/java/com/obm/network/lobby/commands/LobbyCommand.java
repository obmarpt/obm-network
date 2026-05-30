package com.obm.network.lobby.commands;

import com.obm.network.core.combat.CombatLogService;
import com.obm.network.core.combat.CooldownService;
import com.obm.network.lobby.OBMLobbyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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

        // ⚔️ Bloquear se estiver em combate
        if (CombatLogService.isInCombat(player)) {
            player.sendMessage("§cNão podes usar /lobby enquanto estás em combate!");
            return true;
        }

            // ⏱️ Verificar cooldown após entrar noutro modo
            if (!CooldownService.canUseLobby(player)) {
                long rem = CooldownService.getRemainingSeconds(player);
                player.sendMessage("§cAinda não podes voltar ao lobby. Aguarda " + rem + " segundos.");
                return true;
            }

        teleportToLobby(player);
        player.sendMessage("§aFoste teleportado para o lobby.");

        return true;
    }

   private void teleportToLobby(Player player) {

    String worldName = plugin.getConfig().getString("lobby.world");
    World world = Bukkit.getWorld(worldName);

    // ✅ Se ainda não tens o mundo
    if (world == null) {
        player.sendMessage("§cLobby ainda não configurado.");
        plugin.getLogger().warning("⚠ Mundo do lobby não existe ainda: " + worldName);
        return;
    }

    double x = plugin.getConfig().getDouble("lobby.spawn.x");
    double y = plugin.getConfig().getDouble("lobby.spawn.y");
    double z = plugin.getConfig().getDouble("lobby.spawn.z");
    float yaw = (float) plugin.getConfig().getDouble("lobby.spawn.yaw");
    float pitch = (float) plugin.getConfig().getDouble("lobby.spawn.pitch");

    Location spawn = new Location(world, x, y, z, yaw, pitch);
    player.teleport(spawn);
    player.setFireTicks(0);
    try {
        player.setHealth(player.getMaxHealth());
    } catch (Exception ignored) {}
    player.setFoodLevel(20);
    player.setSaturation(20f);
    player.setLevel(0);
    player.setExp(0f);
    player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
}
}

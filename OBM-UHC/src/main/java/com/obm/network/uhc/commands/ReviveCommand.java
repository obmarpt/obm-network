package com.obm.network.uhc.commands;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import com.obm.network.uhc.util.UHCUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ReviveCommand implements CommandExecutor {

    private final DataStore ds = OBMCorePlugin.get().getDataStore();
    private static final String PERMISSION = "uhc.revive";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("Sem permissão.", NamedTextColor.RED));
            return true;
        }

        // ==========================================
        // 1. /revivegeral (Aplica em TODOS os registados como mortos)
        // ==========================================
        if (label.equalsIgnoreCase("revivegeral")) {
            FileConfiguration yaml = ds.getYaml();
            int count = 0;

            if (yaml.getConfigurationSection("players") != null) {
                for (String uuidStr : yaml.getConfigurationSection("players").getKeys(false)) {
                    UUID uuid = UUID.fromString(uuidStr);
                    int lives = yaml.getInt("players." + uuidStr + ".lives_uhc", 0);
                    
                    if (lives <= 0) {
                        yaml.set("players." + uuidStr + ".lives_uhc", 1);
                        yaml.set("players." + uuidStr + ".uhc_pending_revive", "NORMAL");
                        count++;
                    }
                }
                ds.save();
            }

            // Avisar jogadores online que receberam o revive
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(Component.text("Ocorreu um Revive Geral! Quando entrares no UHC estarás vivo.", NamedTextColor.GREEN));
            }

            sender.sendMessage(Component.text("Revive Geral aplicado em " + count + " jogadores na base de dados.", NamedTextColor.GREEN));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Uso correto: /" + label + " <jogador>", NamedTextColor.RED));
            return true;
        }

        // 🚀 USAR OFFLINE PLAYER: Permite carregar o UUID mesmo se o jogador não estiver no servidor
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Component.text("Esse jogador nunca entrou no servidor.", NamedTextColor.RED));
            return true;
        }

        UUID uuid = target.getUniqueId();
        String path = "players." + uuid + ".";
        FileConfiguration yaml = ds.getYaml();

        // ==========================================
        // 2. /revivetotal (Agenda Vidas + Itens + Localização)
        // ==========================================
        if (label.equalsIgnoreCase("revivetotal")) {
            yaml.set(path + "lives_uhc", 1);
            yaml.set(path + "uhc_pending_revive", "TOTAL");
            ds.save(uuid);

            sender.sendMessage(Component.text("Revive Total AGENDADO para " + target.getName() + ". Ele será revivido ao entrar no UHC.", NamedTextColor.GREEN));
            
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(Component.text("Foste revivido com um REVIVE TOTAL! Entra no UHC para voltar ao jogo.", NamedTextColor.GREEN));
            }
            return true;
        }

        // ==========================================
        // 3. /revive (Agenda um revive normal)
        // ==========================================
        if (label.equalsIgnoreCase("revive")) {
            yaml.set(path + "lives_uhc", 1);
            yaml.set(path + "uhc_pending_revive", "NORMAL");
            ds.save(uuid);

            sender.sendMessage(Component.text("Revive Simples aplicado em " + target.getName() + ".", NamedTextColor.GREEN));
            
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(Component.text("Foste revivido! Entra no UHC para voltar ao jogo.", NamedTextColor.GREEN));
            }
            return true;
        }

        return true;
    }
}
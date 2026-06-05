package com.obm.network.lobby;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.lobby.commands.LobbyCommand;
import com.obm.network.lobby.commands.MenuCommand;
import com.obm.network.lobby.gui.MenuManager;
import com.obm.network.lobby.join.JoinHandler;
import com.obm.network.lobby.listener.LobbyItemListener;
import com.obm.network.lobby.retention.ProgressFeedbackTask;
import com.obm.network.lobby.retention.RetentionJoinListener;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class OBMLobbyPlugin extends JavaPlugin {

    private static OBMLobbyPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("OBM-Lobby a iniciar (v" + getDescription().getVersion() + ")...");

        try {
            enablePlugin();
            getLogger().info("OBM-Lobby enabled — comandos: /lobby, /hub, /menu");
        } catch (Exception ex) {
            getLogger().severe("OBM-Lobby falhou ao iniciar — comandos NÃO registados: " + ex.getMessage());
            ex.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void enablePlugin() {
        saveDefaultConfig();

        OBMCorePlugin core = OBMCorePlugin.get();
        if (core == null) {
            getLogger().severe("OBM-Core não está activo — OBM-Lobby requer depend: OBM-Core");
            throw new IllegalStateException("OBM-Core missing");
        }

        if (core.getHologramService() != null) {
            core.getHologramService().reload();
            getLogger().info("Hologramas do lobby geridos pelo OBM-Core (holograms.yml)");
        } else {
            getLogger().warning("HologramService indisponível — verifica DecentHolograms + OBM-Core");
        }

        new JoinHandler(this);

        registerCommand("lobby", new LobbyCommand(this));
        registerCommand("menu", new MenuCommand());

        getServer().getPluginManager().registerEvents(new MenuManager(), this);
        getServer().getPluginManager().registerEvents(new LobbyItemListener(), this);

        ProgressFeedbackTask progressFeedback = new ProgressFeedbackTask();
        getServer().getPluginManager().registerEvents(new RetentionJoinListener(progressFeedback), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("OBM-Lobby disabled");
        instance = null;
    }

    public static OBMLobbyPlugin get() {
        return instance;
    }

    public boolean isCommandDebug() {
        return getConfig().getBoolean("lobby.command.debug", false);
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Comando '/" + name
                    + "' não encontrado — confirma plugin.yml no JAR e faz rebuild (mvn package)");
            return;
        }
        command.setExecutor(executor);
        getLogger().info("Comando '/" + name + "' registado"
                + (command.getAliases().isEmpty() ? "" : " (aliases: " + command.getAliases() + ")"));
    }
}

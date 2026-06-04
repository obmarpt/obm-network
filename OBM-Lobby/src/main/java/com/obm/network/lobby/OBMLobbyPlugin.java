package com.obm.network.lobby;

import com.obm.network.lobby.commands.MenuCommand;
import com.obm.network.lobby.commands.LobbyCommand;
import com.obm.network.lobby.gui.MenuManager;
import com.obm.network.core.OBMCorePlugin;
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

        saveDefaultConfig();

        // Hologramas: OBM-Core → plugins/OBM-Core/holograms.yml (HologramService)
        OBMCorePlugin core = OBMCorePlugin.get();
        if (core != null && core.getHologramService() != null) {
            core.getHologramService().reload();
            getLogger().info("Hologramas do lobby geridos pelo OBM-Core (holograms.yml)");
        } else {
            getLogger().warning("HologramService indisponível — verifica OBM-Core + DecentHolograms");
        }

        // 👋 JOIN HANDLER
        new JoinHandler(this);

        // 🏠 COMANDO /lobby
        registerCommand("lobby", new LobbyCommand(this));

        // 🎮 MENU
        getServer().getPluginManager().registerEvents(new MenuManager(), this);
        registerCommand("menu", new MenuCommand());
        getServer().getPluginManager().registerEvents(new LobbyItemListener(), this);

        ProgressFeedbackTask progressFeedback = new ProgressFeedbackTask();
        getServer().getPluginManager().registerEvents(new RetentionJoinListener(progressFeedback), this);

        getLogger().info("✅ OBM-Lobby iniciado");
    }

    @Override
    public void onDisable() {
        getLogger().info("⛔ OBM-Lobby desligado");
    }

    public static OBMLobbyPlugin get() {
        return instance;
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("⚠️ Comando '/" + name + "' não está declarado no plugin.yml");
        }
    }
}

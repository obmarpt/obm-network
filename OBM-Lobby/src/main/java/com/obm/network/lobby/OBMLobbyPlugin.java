package com.obm.network.lobby;

import com.obm.network.lobby.commands.MenuCommand;
import com.obm.network.lobby.commands.LobbyCommand;
import com.obm.network.lobby.gui.MenuManager;
import com.obm.network.lobby.hologram.TopHologramManager;
import com.obm.network.lobby.join.JoinHandler;
import com.obm.network.lobby.listener.LobbyItemListener;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class OBMLobbyPlugin extends JavaPlugin {

    private static OBMLobbyPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // 🏆 HOLOGRAMAS
        TopHologramManager hologramManager = new TopHologramManager(this);
        hologramManager.createAll();

        // 👋 JOIN HANDLER
        new JoinHandler(this);

        // 🏠 COMANDO /lobby
        registerCommand("lobby", new LobbyCommand(this));

        // 🎮 MENU
        getServer().getPluginManager().registerEvents(new MenuManager(), this);
        registerCommand("menu", new MenuCommand());
        getServer().getPluginManager().registerEvents(new LobbyItemListener(), this);

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

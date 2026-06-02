package com.obm.network.uhc;

import com.obm.network.uhc.actionbar.UHCActionBar;
import com.obm.network.uhc.commands.ReviveCommand;
import com.obm.network.uhc.commands.UHCStatsCommand;
import com.obm.network.uhc.listeners.UHCListener;
import com.obm.network.uhc.manager.UHCManager;
import com.obm.network.uhc.service.UHCReviveService;
import com.obm.network.uhc.task.UHCTimeTracker;
import com.obm.network.uhc.task.UHCTimer;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class OBMUHCPlugin extends JavaPlugin {

    private static OBMUHCPlugin instance;
    private UHCManager uhcManager;
    private UHCReviveService reviveService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        reviveService = new UHCReviveService(this);
        uhcManager = new UHCManager(reviveService);

        Bukkit.getScheduler().runTaskLater(this, uhcManager::ensureHardcoreWorld, 20L);
        Bukkit.getScheduler().runTaskTimer(this, uhcManager::ensureHardcoreWorld, 20L, 1200L);

        registerListeners();
        registerTasks();
        registerCommands();

        getLogger().info("OBM-UHC iniciado com sucesso!");
    }

    @Override
    public void onDisable() {
        instance = null;
        getLogger().info("OBM-UHC desligado");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new UHCListener(uhcManager, reviveService), this);
    }

    private void registerTasks() {
        new UHCTimer(this);
        new UHCActionBar(this);
        new UHCTimeTracker(this);
    }

    private void registerCommands() {
        registerSafeCommand("uhcstats", new UHCStatsCommand());
        ReviveCommand reviveCommand = new ReviveCommand(reviveService);
        registerSafeCommand("revive", reviveCommand);
        registerSafeCommand("revivegeral", reviveCommand);
        registerSafeCommand("revivetotal", reviveCommand);
    }

    private void registerSafeCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("O comando '/" + name + "' não foi registrado no plugin.yml!");
        }
    }

    public static OBMUHCPlugin get() {
        return instance;
    }

    public UHCManager getUHCManager() {
        return uhcManager;
    }

    public UHCReviveService getReviveService() {
        return reviveService;
    }
}

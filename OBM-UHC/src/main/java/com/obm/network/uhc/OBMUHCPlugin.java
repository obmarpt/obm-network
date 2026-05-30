package com.obm.network.uhc;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.uhc.task.UHCTimeTracker;
import com.obm.network.uhc.listeners.UHCListener;
import com.obm.network.uhc.task.UHCTimer;
import com.obm.network.uhc.actionbar.UHCActionBar;
import com.obm.network.uhc.commands.UHCStatsCommand;
import com.obm.network.uhc.commands.ReviveCommand;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class OBMUHCPlugin extends JavaPlugin {

    private static OBMUHCPlugin instance;

    @Override
    public void onEnable() {
        // Inicializa a instância primeiro
        instance = this;

        // ✅ Configuração
        saveDefaultConfig();

        // ✅ Ativar modo hardcore para o UHC
        Bukkit.getScheduler().runTaskLater(this, this::enableHardcoreUHC, 20L);
        Bukkit.getScheduler().runTaskTimer(this, this::enableHardcoreUHC, 20L, 1200L);

        // ✅ Inicialização de Sistemas e Tarefas (Tasks)
        registerListeners();
        registerTasks();
        registerCommands();

        getLogger().info("✅ OBM-UHC iniciado com sucesso!");
    }

    private void enableHardcoreUHC() {
        try {
            String uhcWorld = OBMCorePlugin.get().getWorldModeService().getPrimaryUHCWorld();
            org.bukkit.World world = Bukkit.getWorld(uhcWorld);
            if (world != null) {
                world.setHardcore(true);
                getLogger().info("UHC world set to hardcore: " + uhcWorld);
            } else {
                getLogger().warning("UHC world not loaded yet: " + uhcWorld);
            }
        } catch (Exception e) {
            getLogger().warning("Failed to enable UHC hardcore: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        // 🚨 Previne vazamento de memória (Memory Leak) em caso de /reload
        instance = null; 
        
        getLogger().info("⛔ OBM-UHC desligado");
    }

    /**
     * Centraliza o registro de eventos.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new UHCListener(), this);
    }

    /**
     * Inicializa os loops de repetição e gerenciadores de tempo.
     */
    private void registerTasks() {
        new UHCTimer(this);
        new UHCActionBar(this);
        new UHCTimeTracker(this);
    }

    /**
     * Registra os comandos de forma segura, evitando NullPointerException
     * caso o comando seja esquecido ou digitado errado no plugin.yml.
     */
    private void registerCommands() {
    registerSafeCommand("uhcstats", new UHCStatsCommand());
    
    // Injeta o mesmo executor para as 3 variações do comando de reviver
    ReviveCommand reviveExecutor = new ReviveCommand();
    registerSafeCommand("revive", reviveExecutor);
    registerSafeCommand("revivegeral", reviveExecutor);
    registerSafeCommand("revivetotal", reviveExecutor);
}

    /**
     * Método auxiliar para checar se o comando existe no plugin.yml antes de injetar o Executor.
     */
    private void registerSafeCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("⚠️ O comando '/" + name + "' não foi registrado no plugin.yml!");
        }
    }

    /**
     * Retorna a instância principal do plugin.
     */
    public static OBMUHCPlugin get() {
        return instance;
    }
}
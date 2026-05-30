package com.obm.network.smp;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.commands.MarketCommand;
import com.obm.network.smp.commands.MoneyCommand;
import com.obm.network.smp.commands.RushCommand;
import com.obm.network.smp.commands.SMPCommand;
import com.obm.network.smp.commands.ShopCommand;
import com.obm.network.smp.listener.RushListener;
import com.obm.network.smp.service.EconomyService;
import com.obm.network.smp.service.MarketService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SMPPlugin extends JavaPlugin {

    private static SMPPlugin instance;
    private EconomyService economyService;
    private MarketService marketService;
    private WorldModeService worldModeService;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("❌ Vault não encontrado ou serviço de economia indisponível.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        worldModeService = OBMCorePlugin.get().getWorldModeService();
        economyService = new EconomyService(getEconomy(), getConfig().getInt("starting-balance", 100));
        marketService = new MarketService(this, economyService);

        getServer().getPluginManager().registerEvents(new RushListener(economyService, worldModeService), this);
        // register shop GUI listener
        getServer().getPluginManager().registerEvents(new com.obm.network.smp.gui.RushShopListener(), this);
        scheduleRushRewards();

        registerCommand("smp", new SMPCommand(this));
        registerCommand("rush", new RushCommand(this));
        registerCommand("shop", new ShopCommand(economyService));
        registerCommand("money", new MoneyCommand(economyService));
        registerCommand("market", new MarketCommand(this, marketService, economyService));

        getLogger().info("✅ OBM-SMP iniciado");
    }

    @Override
    public void onDisable() {
        if (marketService != null) {
            marketService.save();
        }
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            return false;
        }
        return provider.getProvider() != null;
    }

    private Economy getEconomy() {
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        return provider == null ? null : provider.getProvider();
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("⚠️ Comando '/" + name + "' não está declarado em plugin.yml");
        }
    }

    public static SMPPlugin get() {
        return instance;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public MarketService getMarketService() {
        return marketService;
    }

    public WorldModeService getWorldModeService() {
        return worldModeService;
    }

    private void scheduleRushRewards() {
        long intervalTicks = 30L * 60L * 20L; // 30 minutes
        int rewardAmount = 25000;

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (!worldModeService.isRush(player.getWorld().getName())) return;
                economyService.deposit(player.getUniqueId(), rewardAmount);
                player.sendMessage("§aVocê recebeu §e" + rewardAmount + " coins §apelo tempo no Rush SMP.");
            });
        }, intervalTicks, intervalTicks);
    }
}

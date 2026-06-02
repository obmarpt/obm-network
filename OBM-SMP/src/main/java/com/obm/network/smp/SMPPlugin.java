package com.obm.network.smp;

import com.obm.network.smp.commands.StatsCommand;

import com.obm.network.smp.listener.StatsListener;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.auction.AuctionGui;
import com.obm.network.smp.commands.AuctionCommand;
import com.obm.network.smp.commands.MarketCommand;
import com.obm.network.smp.commands.MoneyCommand;
import com.obm.network.smp.commands.RankCommand;
import com.obm.network.smp.commands.SMPCommand;
import com.obm.network.smp.commands.SellCommand;
import com.obm.network.smp.commands.ShopCommand;
import com.obm.network.smp.listener.PlayerConnectionListener;
import com.obm.network.smp.listener.SMPListener;
import com.obm.network.smp.listener.SmpGuiListener;
import com.obm.network.smp.listener.SpawnProtectionListener;
import com.obm.network.smp.manager.SMPManager;
import com.obm.network.smp.progression.LevelService;
import com.obm.network.smp.progression.PlayerProgressionStore;
import com.obm.network.smp.progression.ProgressionBonusService;
import com.obm.network.smp.progression.RankCatalog;
import com.obm.network.smp.progression.RankService;
import com.obm.network.smp.sell.SellGui;
import com.obm.network.smp.service.AuctionService;
import com.obm.network.smp.service.EconomyService;
import com.obm.network.smp.service.KillFarmGuard;
import com.obm.network.smp.service.MarketService;
import com.obm.network.smp.service.SellCatalog;
import com.obm.network.smp.service.SellService;
import com.obm.network.smp.service.ShopCatalog;
import com.obm.network.smp.service.ShopService;
import com.obm.network.smp.service.SpawnProtectionService;
import com.obm.network.smp.shop.ShopGui;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SMPPlugin extends JavaPlugin {

    private static SMPPlugin instance;

    private EconomyService economyService;
    private MarketService marketService;
    private SpawnProtectionService spawnProtectionService;
    private ShopCatalog shopCatalog;
    private ShopService shopService;
    private ShopGui shopGui;
    private SellCatalog sellCatalog;
    private SellService sellService;
    private SellGui sellGui;
    private AuctionService auctionService;
    private AuctionGui auctionGui;
    private PlayerProgressionStore progressionStore;
    private RankCatalog rankCatalog;
    private RankService rankService;
    private LevelService levelService;
    private ProgressionBonusService bonusService;
    private KillFarmGuard killFarmGuard;
    private SMPManager smpManager;
    private WorldModeService worldModeService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault não encontrado ou serviço de economia indisponível.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        worldModeService = OBMCorePlugin.get().getWorldModeService();
        economyService = new EconomyService(getEconomy(), getConfig().getInt("starting-balance", 100));
        marketService = new MarketService(this, economyService);
        spawnProtectionService = new SpawnProtectionService(getConfig(), worldModeService);
        killFarmGuard = new KillFarmGuard(getConfig());

        progressionStore = new PlayerProgressionStore();
        rankCatalog = new RankCatalog();
        rankCatalog.reload(getConfig());
        rankService = new RankService(rankCatalog, progressionStore, economyService);
        levelService = new LevelService(progressionStore, economyService::deposit);
        levelService.reload(getConfig());
        bonusService = new ProgressionBonusService(rankService, levelService);

        shopCatalog = new ShopCatalog();
        shopCatalog.reload(getConfig());
        shopService = new ShopService(shopCatalog, economyService, bonusService);
        shopGui = new ShopGui(shopCatalog, shopService);

        sellCatalog = new SellCatalog();
        sellCatalog.reload(getConfig());
        sellService = new SellService(sellCatalog, economyService, bonusService, levelService);
        sellGui = new SellGui(sellService);

        auctionService = new AuctionService(this, marketService, economyService);
        auctionGui = new AuctionGui(auctionService);

        smpManager = new SMPManager(
                economyService,
                worldModeService,
                bonusService,
                levelService,
                progressionStore,
                rankCatalog,
                killFarmGuard,
                getConfig().getInt("economy.kill-reward", 150),
                getConfig().getInt("economy.death-penalty", 50),
                getConfig().getBoolean("economy.death-penalty-enabled", true)
        );

        getServer().getPluginManager().registerEvents(new SMPListener(smpManager, worldModeService), this);
        getServer().getPluginManager().registerEvents(new SpawnProtectionListener(spawnProtectionService), this);
        getServer().getPluginManager().registerEvents(
                new SmpGuiListener(shopGui, shopService, shopCatalog, sellGui, sellService, auctionGui, auctionService),
                this
        );

        schedulePlaytimeRewards();

        Bukkit.getPluginManager().registerEvents(new StatsListener(), this);
        
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(), this);

        registerCommand("stats", new StatsCommand());
        registerCommand("smp", new SMPCommand(this));
        registerCommand("shop", new ShopCommand(shopGui, worldModeService));
        registerCommand("sell", new SellCommand(sellGui, worldModeService));

        AuctionCommand auctionCommand = new AuctionCommand(auctionGui, worldModeService);
        registerCommand("ah", auctionCommand);
        registerCommand("auction", auctionCommand);

        registerCommand("money", new MoneyCommand(economyService));
        registerCommand("market", new MarketCommand(this, marketService, economyService));
        registerCommand("rank", new RankCommand(rankService, levelService, rankCatalog, progressionStore));

      

        // ✅ AUTOSAVE
        Bukkit.getScheduler().runTaskTimer(
                this,
                () -> OBMCorePlugin.get().getDataStore().save(),
                20 * 60,
                20 * 60
        );

        getLogger().info("OBM-SMP iniciado");
    }

    @Override
    public void onDisable() {
        if (marketService != null) {
            marketService.save();
        }
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        return provider != null && provider.getProvider() != null;
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
            getLogger().warning("Comando '/" + name + "' não está declarado em plugin.yml");
        }
    }

    private void schedulePlaytimeRewards() {
        if (!getConfig().getBoolean("rewards.playtime.enabled", true)) {
            return;
        }

        long intervalMinutes = getConfig().getLong("rewards.playtime.interval-minutes", 30L);
        long intervalTicks = intervalMinutes * 60L * 20L;
        int rewardAmount = getConfig().getInt("rewards.playtime.amount", 500);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!worldModeService.isSMP(player.getWorld().getName())) {
                    continue;
                }
                smpManager.handlePlaytimeReward(player, rewardAmount);
            }
        }, intervalTicks, intervalTicks);
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

    public SMPManager getSMPManager() {
        return smpManager;
    }

    public WorldModeService getWorldModeService() {
        return worldModeService;
    }

    public ShopGui getShopGui() {
        return shopGui;
    }

    public AuctionGui getAuctionGui() {
        return auctionGui;
    }

    public SpawnProtectionService getSpawnProtectionService() {
        return spawnProtectionService;
    }

    public RankService getRankService() {
        return rankService;
    }

    public LevelService getLevelService() {
        return levelService;
    }
}

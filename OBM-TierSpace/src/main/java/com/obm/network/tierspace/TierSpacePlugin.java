package com.obm.network.tierspace;

import com.obm.network.core.tier.TierRankUtil;
import com.obm.network.tierspace.anticheat.AnticheatIntegration;
import com.obm.network.tierspace.anticheat.MatchProtectionListener;
import com.obm.network.tierspace.arena.ArenaService;
import com.obm.network.tierspace.command.LeaveCommand;
import com.obm.network.tierspace.command.PartyCommand;
import com.obm.network.tierspace.command.QueueCommand;
import com.obm.network.tierspace.command.TierSpaceCommand;
import com.obm.network.tierspace.feedback.MatchFeedbackService;
import com.obm.network.tierspace.kit.KitService;
import com.obm.network.tierspace.kit.PlayerKitService;
import com.obm.network.tierspace.listener.MatchListener;
import com.obm.network.tierspace.listener.QueueGuiRefreshListener;
import com.obm.network.tierspace.listener.RankedSpawnListener;
import com.obm.network.tierspace.listener.TierGuiListener;
import com.obm.network.tierspace.listener.TierSpaceJoinListener;
import com.obm.network.tierspace.listener.TierSpaceNpcListener;
import com.obm.network.tierspace.match.MatchService;
import com.obm.network.tierspace.match.RematchService;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.party.PartyService;
import com.obm.network.tierspace.progression.DailyQuestService;
import com.obm.network.tierspace.progression.PlacementService;
import com.obm.network.tierspace.queue.QueueFeedbackService;
import com.obm.network.tierspace.queue.QueueService;
import com.obm.network.tierspace.rating.RatingCalculator;
import com.obm.network.tierspace.reward.RankRewardService;
import com.obm.network.tierspace.season.TierSeasonManager;
import com.obm.network.tierspace.storage.TierSpaceStore;
import com.obm.network.tierspace.ui.KitManageGui;
import com.obm.network.tierspace.ui.PartyGui;
import com.obm.network.tierspace.ui.TierGuiMenu;
import com.obm.network.tierspace.ui.TierSpaceScoreboardService;
import com.obm.network.tierspace.ui.TierSpaceTabService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class TierSpacePlugin extends JavaPlugin {

    private static TierSpacePlugin instance;

    private TierSpaceStore store;
    private ModeRegistry modeRegistry;
    private QueueService queueService;
    private QueueFeedbackService queueFeedbackService;
    private MatchService matchService;
    private PartyService partyService;
    private PlayerKitService playerKitService;
    private TierGuiMenu tierGuiMenu;
    private PartyGui partyGui;
    private KitManageGui kitManageGui;
    private TierSeasonManager seasonManager;
    private PlacementService placementService;
    private DailyQuestService dailyQuestService;
    private TierSpaceScoreboardService scoreboardService;
    private TierSpaceTabService tabService;
    private RankRewardService rankRewardService;
    private QueueGuiRefreshListener queueGuiRefreshListener;
    private AnticheatIntegration anticheatIntegration;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        modeRegistry = new ModeRegistry();
        modeRegistry.reload(getConfig());

        int defaultRating = getConfig().getInt("rating.default", TierRankUtil.defaultRating());
        store = new TierSpaceStore(defaultRating);

        int placementGames = getConfig().getInt("placement.games", 10);
        int placementKFactor = getConfig().getInt("placement.k-factor", 40);
        placementService = new PlacementService(store, placementGames, placementKFactor);

        RatingCalculator ratingCalculator = new RatingCalculator(
                getConfig().getInt("rating.k-factor-new", 32),
                getConfig().getInt("rating.k-factor-established", 24),
                getConfig().getInt("rating.established-games", 30),
                placementKFactor,
                getConfig().getDouble("rating.streak-bonus-per-win", 0.10),
                getConfig().getInt("rating.streak-bonus-cap", 5),
                getConfig().getBoolean("demotion-shield.enabled", true),
                getConfig().getInt("demotion-shield.losses-required", 3),
                getConfig().getInt("rating-elo.win-min", 10),
                getConfig().getInt("rating-elo.win-max", 20),
                getConfig().getInt("rating-elo.loss-min", 8),
                getConfig().getInt("rating-elo.loss-max", 15)
        );

        dailyQuestService = new DailyQuestService(
                store,
                getConfig().getInt("daily-quests.wins-required", 3),
                getConfig().getInt("daily-quests.wins-rating-reward", 20),
                getConfig().getInt("daily-quests.matches-required", 5),
                getConfig().getInt("daily-quests.streak-required", 3)
        );

        seasonManager = new TierSeasonManager(this, store, modeRegistry);
        seasonManager.reload(getConfig());
        seasonManager.start();

        rankRewardService = new RankRewardService(this, store, modeRegistry);
        rankRewardService.reload(getConfig());
        rankRewardService.start();

        RematchService rematchService = new RematchService();
        partyService = new PartyService();

        ArenaService arenaService = new ArenaService();
        arenaService.reload(getConfig());

        KitService kitService = new KitService();
        kitService.reload(getConfig());
        playerKitService = new PlayerKitService(kitService);

        MatchFeedbackService feedbackService = new MatchFeedbackService();
        scoreboardService = new TierSpaceScoreboardService(
                this, store, placementService, dailyQuestService, modeRegistry);
        scoreboardService.start();

        tabService = new TierSpaceTabService(this, store, placementService, rankRewardService);
        tabService.start();

        queueService = new QueueService(
                store,
                getConfig().getInt("matchmaking.initial-range", 50),
                getConfig().getInt("matchmaking.range-expansion", 25),
                getConfig().getInt("matchmaking.expansion-interval-seconds", 4),
                getConfig().getInt("matchmaking.max-range", 250)
        );

        tierGuiMenu = new TierGuiMenu(modeRegistry, seasonManager, queueService);
        partyGui = new PartyGui(partyService);
        kitManageGui = new KitManageGui(playerKitService);

        queueFeedbackService = new QueueFeedbackService(this, queueService, store, feedbackService);
        queueFeedbackService.start();

        anticheatIntegration = new AnticheatIntegration(this);
        anticheatIntegration.start();

        matchService = new MatchService(
                this,
                arenaService,
                kitService,
                playerKitService,
                store,
                modeRegistry,
                ratingCalculator,
                feedbackService,
                scoreboardService,
                tabService,
                queueService,
                queueFeedbackService,
                placementService,
                dailyQuestService,
                seasonManager,
                rankRewardService,
                rematchService,
                anticheatIntegration.exemptionService(),
                anticheatIntegration.matchProtectionService(),
                getConfig()
        );

        registerCommands();
        getServer().getPluginManager().registerEvents(new MatchListener(matchService, queueService), this);
        getServer().getPluginManager().registerEvents(
                new MatchProtectionListener(matchService, anticheatIntegration.matchProtectionService()), this);
        getServer().getPluginManager().registerEvents(
                new TierGuiListener(tierGuiMenu, modeRegistry, matchService, rematchService, queueService,
                        queueFeedbackService, partyGui, partyService, kitManageGui, playerKitService), this);
        getServer().getPluginManager().registerEvents(
                new TierSpaceJoinListener(tabService, seasonManager, rankRewardService), this);
        getServer().getPluginManager().registerEvents(
                new RankedSpawnListener(this, tierGuiMenu, partyGui, queueService, queueFeedbackService), this);
        if (getServer().getPluginManager().isPluginEnabled("Citizens")) {
            getServer().getPluginManager().registerEvents(
                    new TierSpaceNpcListener(tierGuiMenu, matchService), this);
        } else {
            getLogger().warning("Citizens não encontrado — NPCs TierSpace desativados.");
        }

        queueGuiRefreshListener = new QueueGuiRefreshListener(this, tierGuiMenu);
        queueGuiRefreshListener.start();

        startMatchmakingTask();

        if (arenaService.getArenaCount() == 0) {
            getLogger().warning("Nenhuma arena configurada.");
        }
        getLogger().info("OBM-TierSpace — " + modeRegistry.enabledModes().size() + " modos PvP activos.");
    }

    @Override
    public void onDisable() {
        if (queueGuiRefreshListener != null) {
            queueGuiRefreshListener.stop();
        }
        if (anticheatIntegration != null) {
            anticheatIntegration.stop();
        }
        if (queueFeedbackService != null) {
            queueFeedbackService.stop();
        }
        if (scoreboardService != null) {
            scoreboardService.stop();
        }
        if (tabService != null) {
            tabService.stop();
        }
        if (rankRewardService != null) {
            rankRewardService.stop();
        }
        if (seasonManager != null) {
            seasonManager.stop();
        }
        instance = null;
    }

    private void registerCommands() {
        registerExecutor("tierspace", new TierSpaceCommand(store, tierGuiMenu, modeRegistry, seasonManager));
        registerExecutor("queue", new QueueCommand(matchService, queueService, queueFeedbackService, modeRegistry));
        registerExecutor("leave", new LeaveCommand(queueService, queueFeedbackService));
        registerExecutor("party", new PartyCommand(partyService));
    }

    private void registerExecutor(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Comando '/" + name + "' não declarado no plugin.yml");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    private void startMatchmakingTask() {
        long interval = getConfig().getLong("matchmaking.tick-interval-seconds", 1L) * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () ->
                queueService.findMatch().ifPresent(matchService::startMatch), interval, interval);
    }

    public static TierSpacePlugin get() {
        return instance;
    }

    public MatchService getMatchService() {
        return matchService;
    }

    public QueueService getQueueService() {
        return queueService;
    }

    public PartyService getPartyService() {
        return partyService;
    }

    public TierSpaceStore getStore() {
        return store;
    }

    public ModeRegistry getModeRegistry() {
        return modeRegistry;
    }

    public TierGuiMenu getTierGuiMenu() {
        return tierGuiMenu;
    }

    public PlacementService getPlacementService() {
        return placementService;
    }

    public DailyQuestService getDailyQuestService() {
        return dailyQuestService;
    }

    public TierSeasonManager getSeasonManager() {
        return seasonManager;
    }
}

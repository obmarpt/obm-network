package com.obm.network.tierspace.match;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.integration.EmeraldRewardBridge;
import com.obm.network.core.location.LobbySpawnService;
import com.obm.network.core.state.PlayerStateBridge;
import com.obm.network.core.state.TierSpaceStateHelper;
import com.obm.network.core.tier.TierRankUtil;
import com.obm.network.tierspace.TierSpacePlugin;
import com.obm.network.tierspace.anticheat.AnticheatExemptionService;
import com.obm.network.tierspace.anticheat.MatchProtectionService;
import com.obm.network.tierspace.arena.ArenaDefinition;
import com.obm.network.tierspace.arena.ArenaService;
import com.obm.network.tierspace.feedback.MatchFeedbackService;
import com.obm.network.tierspace.kit.KitService;
import com.obm.network.tierspace.kit.PlayerKitService;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import com.obm.network.tierspace.progression.DailyQuestService;
import com.obm.network.tierspace.progression.PlacementService;
import com.obm.network.tierspace.queue.QueueService;
import com.obm.network.tierspace.queue.QueueFeedbackService;
import com.obm.network.tierspace.rating.RatingCalculator;
import com.obm.network.tierspace.rating.RatingChange;
import com.obm.network.tierspace.reward.RankRewardService;
import com.obm.network.tierspace.season.TierSeasonManager;
import com.obm.network.tierspace.storage.TierSpaceStore;
import com.obm.network.tierspace.ui.PostMatchGui;
import com.obm.network.tierspace.ui.PostMatchSnapshot;
import com.obm.network.tierspace.ui.TierSpaceScoreboardService;
import com.obm.network.tierspace.ui.TierSpaceTabService;
import com.obm.network.tierspace.hub.TierSpaceHub;
import com.obm.network.tierspace.hub.TierSpaceHubService;
import com.obm.network.tierspace.util.TierSpaceLog;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MatchService {

    private final TierSpacePlugin plugin;
    private final ArenaService arenaService;
    private final KitService kitService;
    private final PlayerKitService playerKitService;
    private final TierSpaceStore store;
    private final ModeRegistry modeRegistry;
    private final RatingCalculator ratingCalculator;
    private final MatchFeedbackService feedbackService;
    private final TierSpaceScoreboardService scoreboardService;
    private final TierSpaceTabService tabService;
    private final QueueService queueService;
    private final QueueFeedbackService queueFeedbackService;
    private final PlacementService placementService;
    private final DailyQuestService dailyQuestService;
    private final TierSeasonManager seasonManager;
    private final RankRewardService rankRewardService;
    private final RematchService rematchService;
    private final AnticheatExemptionService anticheatExemptionService;
    private final MatchProtectionService matchProtectionService;
    private final int countdownSeconds;
    private final int postMatchDelayTicks;
    private final int postMatchGuiDelayTicks;
    private final int dailyWinBonus;
    private final int demotionShieldLosses;
    private final Map<UUID, Match> activeMatches = new ConcurrentHashMap<>();
    private final Map<String, Integer> liveBarTasks = new ConcurrentHashMap<>();
    private final Map<UUID, GameModeId> postMatchModes = new ConcurrentHashMap<>();
    private final Map<UUID, PostMatchSnapshot> postMatchSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> restoreTaskIds = new ConcurrentHashMap<>();
    private final Set<UUID> postMatchKitLocked = ConcurrentHashMap.newKeySet();

    public MatchService(TierSpacePlugin plugin,
                        ArenaService arenaService,
                        KitService kitService,
                        PlayerKitService playerKitService,
                        TierSpaceStore store,
                        ModeRegistry modeRegistry,
                        RatingCalculator ratingCalculator,
                        MatchFeedbackService feedbackService,
                        TierSpaceScoreboardService scoreboardService,
                        TierSpaceTabService tabService,
                        QueueService queueService,
                        QueueFeedbackService queueFeedbackService,
                        PlacementService placementService,
                        DailyQuestService dailyQuestService,
                        TierSeasonManager seasonManager,
                        RankRewardService rankRewardService,
                        RematchService rematchService,
                        AnticheatExemptionService anticheatExemptionService,
                        MatchProtectionService matchProtectionService,
                        FileConfiguration config) {
        this.plugin = plugin;
        this.arenaService = arenaService;
        this.kitService = kitService;
        this.playerKitService = playerKitService;
        this.store = store;
        this.modeRegistry = modeRegistry;
        this.ratingCalculator = ratingCalculator;
        this.feedbackService = feedbackService;
        this.scoreboardService = scoreboardService;
        this.tabService = tabService;
        this.queueService = queueService;
        this.queueFeedbackService = queueFeedbackService;
        this.placementService = placementService;
        this.dailyQuestService = dailyQuestService;
        this.seasonManager = seasonManager;
        this.rankRewardService = rankRewardService;
        this.rematchService = rematchService;
        this.anticheatExemptionService = anticheatExemptionService;
        this.matchProtectionService = matchProtectionService;
        this.countdownSeconds = config.getInt("match.countdown-seconds", 3);
        this.postMatchDelayTicks = config.getInt("match.post-match-delay-ticks", 60);
        this.postMatchGuiDelayTicks = config.getInt("match.post-match-gui-delay-ticks", 60);
        this.dailyWinBonus = config.getInt("rewards.daily-first-win-bonus", 15);
        this.demotionShieldLosses = config.getInt("demotion-shield.losses-required", 3);
    }

    public GameModeId getPostMatchMode(UUID uuid) {
        return postMatchModes.getOrDefault(uuid, GameModeId.SWORD);
    }

    public void clearPostMatchMode(UUID uuid) {
        postMatchModes.remove(uuid);
    }

    public boolean joinQueue(Player player, GameModeId mode) {
        if (!com.obm.network.core.security.SecurityBridge.allowQueueJoin(player)) {
            return false;
        }
        if (!modeRegistry.isEnabled(mode)) {
            player.sendMessage("§cEste modo não está disponível.");
            return false;
        }
        if (isInMatch(player.getUniqueId())) {
            player.sendMessage("§cJá estás num match.");
            return false;
        }
        seasonManager.ensurePlayerSeason(player);
        return enterQueue(player, mode, false);
    }

    public boolean switchQueue(Player player, GameModeId mode) {
        if (!com.obm.network.core.security.SecurityBridge.allowQueueJoin(player)) {
            return false;
        }
        if (!modeRegistry.isEnabled(mode)) {
            player.sendMessage("§cEste modo não está disponível.");
            return false;
        }
        if (isInMatch(player.getUniqueId())) {
            player.sendMessage("§cJá estás num match.");
            return false;
        }
        seasonManager.ensurePlayerSeason(player);
        return enterQueue(player, mode, false);
    }

    public boolean requeue(Player player, GameModeId mode) {
        return requeue(player, mode, true);
    }

    private boolean requeue(Player player, GameModeId mode, boolean fromPostMatch) {
        if (!fromPostMatch && !com.obm.network.core.security.SecurityBridge.allowQueueJoin(player)) {
            return false;
        }
        if (isInMatch(player.getUniqueId())) {
            player.sendMessage("§cAinda estás num match.");
            return false;
        }

        QueueService.JoinResult result = queueService.joinOrSwitch(player, mode);
        store.ensureInitialized(player.getUniqueId(), mode);
        queueFeedbackService.startTracking(player);

        String modeName = modeRegistry.displayName(mode);
        int inQueue = queueService.getQueueSize(mode);
        switch (result) {
            case JOINED -> {
                if (fromPostMatch) {
                    feedbackService.sendRequeue(player);
                } else {
                    feedbackService.sendQueueJoin(player, modeName, inQueue);
                }
            }
            case SWITCHED -> player.sendMessage("§dTierSpace §8| §7Fila: §f" + modeName
                    + " §8(§7" + inQueue + " na queue§8)");
            case REFRESHED -> player.sendMessage("§dTierSpace §8| §aFila atualizada: §f" + modeName);
        }
        PlayerStateBridge.setQueue(player);
        return true;
    }

    private boolean enterQueue(Player player, GameModeId mode, boolean fromPostMatch) {
        if (isInMatch(player.getUniqueId())) {
            player.sendMessage("§cJá estás num match.");
            return false;
        }
        return requeue(player, mode, fromPostMatch);
    }

    public Optional<Match> getMatch(UUID uuid) {
        return Optional.ofNullable(activeMatches.get(uuid));
    }

    public boolean isInMatch(UUID uuid) {
        return activeMatches.containsKey(uuid);
    }

    public boolean isKitLocked(UUID uuid) {
        return uuid != null && (activeMatches.containsKey(uuid) || postMatchKitLocked.contains(uuid));
    }

    public void shutdown() {
        for (UUID uuid : new java.util.ArrayList<>(activeMatches.keySet())) {
            Optional<Match> match = getMatch(uuid);
            match.ifPresent(this::cleanupMatch);
            clearPostMatchState(uuid);
        }
        activeMatches.clear();
        postMatchKitLocked.clear();
        restoreTaskIds.clear();
        postMatchSnapshots.clear();
        postMatchModes.clear();
    }

    public void clearPostMatchState(UUID uuid) {
        if (uuid == null) {
            return;
        }
        postMatchKitLocked.remove(uuid);
        postMatchSnapshots.remove(uuid);
        postMatchModes.remove(uuid);
        cancelPendingRestore(uuid);
    }

    public void startMatch(QueueService.MatchPair pair) {
        Player playerOne = Bukkit.getPlayer(pair.first().uuid());
        Player playerTwo = Bukkit.getPlayer(pair.second().uuid());
        GameModeId mode = pair.first().mode();
        if (playerOne == null || !playerOne.isOnline()) {
            requeueAfterFailedMatch(playerTwo, mode);
            return;
        }
        if (playerTwo == null || !playerTwo.isOnline()) {
            requeueAfterFailedMatch(playerOne, mode);
            return;
        }
        beginMatch(playerOne, playerTwo, mode);
    }

    private void requeueAfterFailedMatch(Player player, GameModeId mode) {
        if (player == null || !player.isOnline()) {
            return;
        }
        queueService.joinOrSwitch(player, mode);
        queueFeedbackService.startTracking(player);
        player.sendMessage("§eOponente indisponível — voltaste à fila.");
    }

    public void startDirectMatch(Player playerOne, Player playerTwo, GameModeId mode) {
        if (playerOne == null || playerTwo == null || !playerOne.isOnline() || !playerTwo.isOnline()) {
            return;
        }
        if (isInMatch(playerOne.getUniqueId()) || isInMatch(playerTwo.getUniqueId())) {
            playerOne.sendMessage("§cUm dos jogadores ainda está num match.");
            playerTwo.sendMessage("§cUm dos jogadores ainda está num match.");
            return;
        }
        queueService.leave(playerOne.getUniqueId());
        queueService.leave(playerTwo.getUniqueId());
        beginMatch(playerOne, playerTwo, mode);
    }

    private void beginMatch(Player playerOne, Player playerTwo, GameModeId mode) {
        if (isInMatch(playerOne.getUniqueId()) || isInMatch(playerTwo.getUniqueId())) {
            playerOne.sendMessage("§cUm dos jogadores ainda está num match.");
            playerTwo.sendMessage("§cUm dos jogadores ainda está num match.");
            return;
        }
        seasonManager.ensurePlayerSeason(playerOne);
        seasonManager.ensurePlayerSeason(playerTwo);

        Optional<ArenaDefinition> arenaOptional = arenaService.acquireArena(mode);
        if (arenaOptional.isEmpty()) {
            int total = arenaService.getArenaCount(mode);
            int free = arenaService.getAvailableCount(mode);
            String detail = total == 0
                    ? "§7Nenhuma arena configurada para este modo."
                    : "§7Arenas: §f" + free + "§7/§f" + total + " §7livres.";
            playerOne.sendMessage("§cNenhuma arena livre para §f" + modeRegistry.displayName(mode) + "§c.");
            playerOne.sendMessage(detail);
            playerTwo.sendMessage("§cNenhuma arena livre para §f" + modeRegistry.displayName(mode) + "§c.");
            playerTwo.sendMessage(detail);
            queueService.joinOrSwitch(playerOne, mode);
            queueService.joinOrSwitch(playerTwo, mode);
            queueFeedbackService.startTracking(playerOne);
            queueFeedbackService.startTracking(playerTwo);
            return;
        }

        ArenaDefinition arena = arenaOptional.get();
        String kitId = modeRegistry.kitId(mode);
        TierSpaceLog.info("Match start " + playerOne.getName() + " vs " + playerTwo.getName()
                + " mode=" + mode.id() + " arena=" + arena.id());

        Match match = new Match(
                UUID.randomUUID().toString(),
                mode,
                arena.id(),
                playerOne.getUniqueId(),
                playerTwo.getUniqueId(),
                playerOne.getLocation().clone(),
                playerTwo.getLocation().clone(),
                playerOne.getInventory().getContents(),
                playerTwo.getInventory().getContents(),
                playerOne.getInventory().getArmorContents(),
                playerTwo.getInventory().getArmorContents(),
                playerOne.getInventory().getItemInOffHand(),
                playerTwo.getInventory().getItemInOffHand()
        );

        activeMatches.put(playerOne.getUniqueId(), match);
        activeMatches.put(playerTwo.getUniqueId(), match);

        queueFeedbackService.stopTracking(match.playerOne());
        queueFeedbackService.stopTracking(match.playerTwo());

        scoreboardService.track(playerOne, mode);
        scoreboardService.track(playerTwo, mode);

        int ratingOne = store.getRating(playerOne.getUniqueId(), mode);
        int ratingTwo = store.getRating(playerTwo.getUniqueId(), mode);
        feedbackService.sendMatchFound(playerOne, playerTwo, ratingOne, ratingTwo);
        feedbackService.sendMatchFound(playerTwo, playerOne, ratingTwo, ratingOne);

        playerOne.teleport(arena.spawn1());
        playerTwo.teleport(arena.spawn2());

        onMatchStart(playerOne);
        onMatchStart(playerTwo);
        applyMatchCombatState(playerOne);
        applyMatchCombatState(playerTwo);
        playerKitService.applyForMatch(playerOne, kitId);
        playerKitService.applyForMatch(playerTwo, kitId);

        runCountdown(match, countdownSeconds);
    }

    public void finishMatch(UUID winnerId, UUID loserId, Match.EndReason reason) {
        Match match = activeMatches.get(winnerId);
        if (match == null || match.state() == Match.State.FINISHED) {
            return;
        }
        match.setState(Match.State.FINISHED);

        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(loserId);
        GameModeId mode = match.mode();

        int winnerRating = store.getRating(winnerId, mode);
        int loserRating = store.getRating(loserId, mode);
        int preWinStreak = store.getStreak(winnerId, mode);
        int preLossStreak = store.getStreak(loserId, mode);
        boolean winnerInPlacement = placementService.isInPlacement(winnerId, mode);
        boolean loserInPlacement = placementService.isInPlacement(loserId, mode);

        if (reason == Match.EndReason.DEATH) {
            store.recordMatchKill(winnerId, loserId, mode);
        }

        RatingChange winChange = ratingCalculator.calculateWin(
                winnerRating,
                loserRating,
                store.getGamesPlayed(winnerId, mode),
                preWinStreak,
                winnerInPlacement
        );

        if (store.claimDailyWin(winnerId, mode)) {
            winChange = ratingCalculator.applyDailyBonus(winChange, dailyWinBonus);
        }

        RatingChange lossChange = ratingCalculator.calculateLoss(
                loserRating,
                winnerRating,
                store.getGamesPlayed(loserId, mode),
                preLossStreak,
                store.getRatingLossStreak(loserId, mode),
                loserInPlacement
        );

        store.recordWin(winnerId, mode, winChange.newRating(), winChange.newStreak());
        store.recordLoss(loserId, mode, lossChange.newRating(), lossChange.newRatingLossStreak());

        dailyQuestService.recordMatch(winnerId, mode);
        dailyQuestService.recordMatch(loserId, mode);
        dailyQuestService.recordWin(winnerId, mode, winChange.newStreak());

        boolean winnerPlaced = placementService.recordMatch(winnerId, mode);
        boolean loserPlaced = placementService.recordMatch(loserId, mode);

        rematchService.registerFinishedMatch(match.playerOne(), match.playerTwo(), mode);

        stopLiveActionBar(match.id());

        String winnerOpponent = loser != null ? loser.getName() : "oponente";
        String loserOpponent = winner != null ? winner.getName() : "oponente";

        if (winner != null) {
            EmeraldRewardBridge.rankedWin(winner);
            com.obm.network.core.integration.BattlePassBridge.rankedWin(winner);
            com.obm.network.core.integration.BattlePassBridge.rankedMatch(winner);
            postMatchSnapshots.put(winnerId, PostMatchSnapshot.win(winChange, winnerInPlacement));
            feedbackService.sendWin(winner, winChange, winnerOpponent, winnerInPlacement);
            if (winnerPlaced) {
                placementService.sendRankReveal(winner, mode);
            }
            rankRewardService.refresh(winner);
            scoreboardService.updatePlayer(winner, mode);
            tabService.refresh(winner);
        }
        if (loser != null) {
            com.obm.network.core.integration.BattlePassBridge.rankedMatch(loser);
            postMatchSnapshots.put(loserId, PostMatchSnapshot.defeat(lossChange, loserInPlacement));
            feedbackService.sendLoss(loser, lossChange, loserOpponent, loserInPlacement, demotionShieldLosses);
            if (loserPlaced) {
                placementService.sendRankReveal(loser, mode);
            }
            rankRewardService.refresh(loser);
            scoreboardService.updatePlayer(loser, mode);
            tabService.refresh(loser);
        }

        UUID playerOneId = match.playerOne();
        UUID playerTwoId = match.playerTwo();

        onMatchEnd(Bukkit.getPlayer(playerOneId));
        onMatchEnd(Bukkit.getPlayer(playerTwoId));

        activeMatches.remove(playerOneId);
        activeMatches.remove(playerTwoId);
        postMatchKitLocked.add(playerOneId);
        postMatchKitLocked.add(playerTwoId);
        scoreboardService.untrack(playerOneId);
        scoreboardService.untrack(playerTwoId);
        arenaService.releaseArena(match.arenaId());

        scheduleRestorePlayers(match);
        Bukkit.getScheduler().runTaskLater(plugin, () -> openPostMatchGui(playerOneId, mode), postMatchGuiDelayTicks);
        Bukkit.getScheduler().runTaskLater(plugin, () -> openPostMatchGui(playerTwoId, mode), postMatchGuiDelayTicks);
    }

    private void openPostMatchGui(UUID uuid, GameModeId mode) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline() || isInMatch(uuid)) {
            return;
        }
        postMatchModes.put(uuid, mode);
        PostMatchSnapshot snapshot = postMatchSnapshots.remove(uuid);
        if (snapshot == null) {
            snapshot = new PostMatchSnapshot(false, 0, store.getRating(uuid, mode),
                    TierRankUtil.fromRating(store.getRating(uuid, mode)).displayName(),
                    store.getStreak(uuid, mode), placementService.isInPlacement(uuid, mode));
        }
        PostMatchGui.open(player, mode, rematchService, snapshot);
    }

    public void handleDeath(Player victim, Player killer) {
        Optional<Match> matchOptional = getMatch(victim.getUniqueId());
        if (matchOptional.isEmpty() || matchOptional.get().state() != Match.State.FIGHTING) {
            return;
        }

        Match match = matchOptional.get();
        UUID winnerId = killer != null ? killer.getUniqueId() : match.opponent(victim.getUniqueId());
        if (winnerId == null) {
            return;
        }
        finishMatch(winnerId, victim.getUniqueId(), Match.EndReason.DEATH);
    }

    public void handleDisconnect(Player player) {
        Optional<Match> matchOptional = getMatch(player.getUniqueId());
        if (matchOptional.isEmpty()) {
            return;
        }

        Match match = matchOptional.get();
        if (match.state() == Match.State.FINISHED) {
            return;
        }

        UUID opponentId = match.opponent(player.getUniqueId());
        if (opponentId != null) {
            finishMatch(opponentId, player.getUniqueId(), Match.EndReason.DISCONNECT);
        } else {
            cleanupMatch(match);
        }
    }

    public void teleportToLobby(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        clearPostMatchState(uuid);
        clearMatchKit(player);
        TierSpaceStateHelper.leaveQueueIfQueued(player);
        if (!LobbySpawnService.teleportToLobby(player)) {
            player.sendMessage("§cLobby não encontrado.");
            return;
        }
        player.sendMessage("§aVoltaste ao lobby.");
    }

    public void clearMatchKit(Player player) {
        if (player == null) {
            return;
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setItemOnCursor(null);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }

    public void cancelPendingRestore(UUID uuid) {
        if (uuid == null) {
            return;
        }
        Integer taskId = restoreTaskIds.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private void scheduleRestorePlayers(Match match) {
        UUID one = match.playerOne();
        UUID two = match.playerTwo();
        cancelPendingRestore(one);
        cancelPendingRestore(two);
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> restorePlayers(match), postMatchDelayTicks)
                .getTaskId();
        restoreTaskIds.put(one, taskId);
        restoreTaskIds.put(two, taskId);
    }

    private void runCountdown(Match match, int secondsLeft) {
        Player playerOne = Bukkit.getPlayer(match.playerOne());
        Player playerTwo = Bukkit.getPlayer(match.playerTwo());
        if (playerOne == null || playerTwo == null) {
            cleanupMatch(match);
            return;
        }

        if (secondsLeft == countdownSeconds) {
            matchProtectionService.applyCountdownProtection(playerOne);
            matchProtectionService.applyCountdownProtection(playerTwo);
        }

        if (secondsLeft > 0) {
            feedbackService.sendCountdown(playerOne, secondsLeft);
            feedbackService.sendCountdown(playerTwo, secondsLeft);
            Bukkit.getScheduler().runTaskLater(plugin, () -> runCountdown(match, secondsLeft - 1), 20L);
            return;
        }

        matchProtectionService.releaseCountdownProtection(playerOne);
        matchProtectionService.releaseCountdownProtection(playerTwo);

        match.setState(Match.State.FIGHTING);
        feedbackService.sendFightStart(playerOne);
        feedbackService.sendFightStart(playerTwo);
        startLiveActionBar(match);
    }

    private void startLiveActionBar(Match match) {
        stopLiveActionBar(match.id());

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (match.state() != Match.State.FIGHTING) {
                stopLiveActionBar(match.id());
                return;
            }

            Player playerOne = Bukkit.getPlayer(match.playerOne());
            Player playerTwo = Bukkit.getPlayer(match.playerTwo());

            sendLiveBar(playerOne, playerTwo, match);
            sendLiveBar(playerTwo, playerOne, match);
        }, 20L, 20L).getTaskId();

        liveBarTasks.put(match.id(), taskId);
    }

    private void sendLiveBar(Player player, Player opponent, Match match) {
        if (player == null || !player.isOnline()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        GameModeId mode = match.mode();
        int rating = store.getRating(uuid, mode);
        int streak = store.getStreak(uuid, mode);

        String rankLabel;
        if (placementService.isInPlacement(uuid, mode)) {
            rankLabel = placementService.getPlacementLabel(uuid, mode);
        } else {
            rankLabel = TierRankUtil.fromRating(rating).displayName();
        }

        double ownHp = Math.max(0, player.getHealth());
        double oppHp = opponent != null && opponent.isOnline() ? Math.max(0, opponent.getHealth()) : 0;
        feedbackService.sendCombatBar(player, ownHp, oppHp, modeRegistry.displayName(mode), rating, streak, rankLabel);
    }

    private void stopLiveActionBar(String matchId) {
        Integer taskId = liveBarTasks.remove(matchId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private void restorePlayers(Match match) {
        restorePlayer(Bukkit.getPlayer(match.playerOne()), match.returnOne(),
                match.backupOne(), match.armorOne(), match.offhandOne());
        restorePlayer(Bukkit.getPlayer(match.playerTwo()), match.returnTwo(),
                match.backupTwo(), match.armorTwo(), match.offhandTwo());
    }

    private void restorePlayer(Player player, org.bukkit.Location location,
                               ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.setGameMode(GameMode.SURVIVAL);
        if (location != null) {
            player.teleport(location);
        }
        String currentWorld = player.getWorld().getName();
        if (TierSpaceHub.isInTierSpaceHub(currentWorld)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || isInMatch(player.getUniqueId())) {
                    return;
                }
                TierSpaceHubService hub = plugin.getHubService();
                if (hub != null) {
                    hub.handleTierSpaceJoin(player, true);
                }
                PlayerStateBridge.setTierSpace(player);
                restoreTaskIds.remove(player.getUniqueId());
                postMatchKitLocked.remove(player.getUniqueId());
            }, 2L);
            return;
        }
        clearMatchKit(player);
        postMatchKitLocked.remove(player.getUniqueId());
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armor);
        if (offhand != null) {
            player.getInventory().setItemInOffHand(offhand);
        }
    }

    private void onMatchStart(Player player) {
        matchProtectionService.onMatchStart(player);
        if (anticheatExemptionService.isIntegrationActive()) {
            anticheatExemptionService.handleAnticheatExemption(player);
        }
    }

    private void applyMatchCombatState(Player player) {
        PlayerStateBridge.enterMatchCombat(player);
    }

    private void onMatchEnd(Player player) {
        if (player != null) {
            matchProtectionService.onMatchEnd(player);
            if (!isInMatch(player.getUniqueId())) {
                if (TierSpaceHub.isInTierSpaceHub(player.getWorld().getName())) {
                    PlayerStateBridge.setTierSpace(player);
                } else {
                    PlayerStateBridge.syncFromWorld(player);
                }
            }
        }
    }

    public void leaveQueueState(Player player) {
        if (player != null && !com.obm.network.core.security.SecurityBridge.allowQueueLeave(player)) {
            return;
        }
        if (player == null || isInMatch(player.getUniqueId())) {
            return;
        }
        PlayerStateBridge.leaveQueue(player);
    }

    private void cleanupMatch(Match match) {
        stopLiveActionBar(match.id());
        onMatchEnd(Bukkit.getPlayer(match.playerOne()));
        onMatchEnd(Bukkit.getPlayer(match.playerTwo()));
        activeMatches.remove(match.playerOne());
        activeMatches.remove(match.playerTwo());
        scoreboardService.untrack(match.playerOne());
        scoreboardService.untrack(match.playerTwo());
        arenaService.releaseArena(match.arenaId());
        restorePlayers(match);
    }
}

package com.obm.network.smp.duel;

import com.obm.network.core.combat.CombatLogService;
import com.obm.network.core.ui.PlayerUx;
import com.obm.network.core.location.SafeSpawnService;
import com.obm.network.core.state.PlayerStateBridge;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.SMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DuelService {

    private final SMPPlugin plugin;
    private final WorldModeService worldModeService;
    private final DuelArenaService arenaService;
    private final DuelPendingStore pendingStore;

    private final Map<UUID, DuelInvite> pendingInvites = new ConcurrentHashMap<>();
    private final Map<UUID, Duel> activeDuels = new ConcurrentHashMap<>();
    private final Map<String, Integer> countdownTasks = new ConcurrentHashMap<>();

    private final boolean enabled;
    private final long inviteTimeoutMs;
    private final long maxDurationMs;
    private final long roundTimeoutMs;
    private final int maxRounds;
    private final boolean returnToOriginal;
    private final int countdownSeconds;

    private BukkitTask tickTask;

    public DuelService(SMPPlugin plugin, WorldModeService worldModeService, DuelArenaService arenaService) {
        this.plugin = plugin;
        this.worldModeService = worldModeService;
        this.arenaService = arenaService;
        this.pendingStore = new DuelPendingStore(plugin);

        var cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("duel.enabled", true);
        this.inviteTimeoutMs = cfg.getLong("duel.invite-timeout-seconds", 60L) * 1000L;
        this.maxDurationMs = cfg.getLong("duel.max-duration-seconds", 300L) * 1000L;
        this.roundTimeoutMs = cfg.getLong("duel.round-timeout-seconds", 120L) * 1000L;
        this.maxRounds = cfg.getInt("duel.max-rounds", 3);
        this.returnToOriginal = cfg.getBoolean("duel.return-to-original-position", true);
        this.countdownSeconds = cfg.getInt("duel.round-countdown-seconds", 3);

        reconcileOrphans();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isInDuel(UUID uuid) {
        return activeDuels.containsKey(uuid);
    }

    public boolean isRoundActive(UUID uuid) {
        return getDuel(uuid).map(d -> d.state() == DuelState.ACTIVE).orElse(false);
    }

    public Optional<Duel> getDuel(UUID uuid) {
        return Optional.ofNullable(activeDuels.get(uuid));
    }

    public boolean isDuelWorld(String worldName) {
        return arenaService.isDuelWorld(worldName);
    }

    public void trackBrokenItem(UUID playerId, ItemStack broken) {
        getDuel(playerId).ifPresent(duel -> {
            DuelItemLedger ledger = duel.ledgerFor(playerId);
            if (ledger != null) {
                ledger.trackBroken(broken);
            }
        });
    }

    public void trackSecuredDrop(UUID playerId, ItemStack dropped) {
        getDuel(playerId).ifPresent(duel -> {
            DuelItemLedger ledger = duel.ledgerFor(playerId);
            if (ledger != null) {
                ledger.trackSecuredDrop(dropped);
            }
        });
    }

    public void handleJoin(Player player) {
        deliverPendingReturns(player);
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (String duelId : List.copyOf(countdownTasks.keySet())) {
            cancelCountdown(duelId);
        }
        for (Duel duel : uniqueActiveDuels()) {
            stashOfflineLedgers(duel);
            forceEnd(duel, DuelEndReason.CANCELLED);
        }
        pendingInvites.clear();
        pendingStore.saveSync();
    }

    public void challenge(Player challenger, Player target) {
        if (!enabled) {
            PlayerUx.error(challenger, "Duelos estão desativados.");
            return;
        }
        if (challenger.equals(target)) {
            PlayerUx.error(challenger, "Não podes desafiar-te a ti mesmo.");
            return;
        }
        String blockReason = duelBlockReason(challenger);
        if (blockReason != null) {
            PlayerUx.error(challenger, blockReason);
            PlayerUx.errorSound(challenger);
            return;
        }
        blockReason = duelBlockReason(target);
        if (blockReason != null) {
            PlayerUx.error(challenger, target.getName() + " não pode duelar agora.");
            PlayerUx.hint(challenger, "Espera que saia de combate, queue ou outro duelo.");
            PlayerUx.errorSound(challenger);
            return;
        }
        if (!worldModeService.isSMP(challenger.getWorld().getName())) {
            PlayerUx.error(challenger, "Só podes desafiar no mundo SMP.");
            return;
        }
        if (!worldModeService.isSMP(target.getWorld().getName())) {
            PlayerUx.error(challenger, target.getName() + " não está no SMP.");
            return;
        }
        if (pendingInvites.containsKey(target.getUniqueId())) {
            DuelInvite existing = pendingInvites.get(target.getUniqueId());
            if (existing != null && existing.challenger().equals(challenger.getUniqueId())) {
                PlayerUx.info(challenger, "Já enviaste um convite a §f" + target.getName() + "§e.");
                PlayerUx.hint(challenger, "Aguarda resposta ou expira em breve.");
                return;
            }
            PlayerUx.error(challenger, target.getName() + " já tem um convite pendente.");
            return;
        }
        if (pendingInvites.containsKey(challenger.getUniqueId())) {
            PlayerUx.error(challenger, "Tens um convite pendente.");
            PlayerUx.hint(challenger, "Usa §f/duel accept §7ou §f/duel deny§7.");
            return;
        }

        long expiresAt = System.currentTimeMillis() + inviteTimeoutMs;
        DuelInvite invite = new DuelInvite(challenger.getUniqueId(), target.getUniqueId(), expiresAt);
        pendingInvites.put(target.getUniqueId(), invite);

        int timeoutSec = (int) Math.max(1L, inviteTimeoutMs / 1000L);
        PlayerUx.success(challenger, "Convite BO3 enviado a §f" + target.getName() + "§a!");
        PlayerUx.hint(challenger, "Expira em §f" + timeoutSec + "s§7.");
        PlayerUx.confirmSound(challenger);

        PlayerUx.info(target, "§f" + challenger.getName() + " §equer duelar contigo (BO3)!");
        PlayerUx.hint(target, "§f/duel accept §7— aceitar  §8|  §f/duel deny §7— recusar");
        PlayerUx.actionBar(target, "§6⚔ Duelo: §f" + challenger.getName() + " §7te desafiou!");
        target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.3f);
    }

    public void accept(Player player) {
        DuelInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null || invite.isExpired()) {
            PlayerUx.error(player, "Não tens convites de duelo activos.");
            PlayerUx.errorSound(player);
            return;
        }
        Player challenger = Bukkit.getPlayer(invite.challenger());
        if (challenger == null || !challenger.isOnline()) {
            PlayerUx.error(player, "O desafiante já não está online.");
            return;
        }
        String blockReason = duelBlockReason(challenger);
        if (blockReason != null || duelBlockReason(player) != null) {
            PlayerUx.error(player, "Não é possível iniciar o duelo agora.");
            PlayerUx.error(challenger, "O duelo foi cancelado — condições inválidas.");
            PlayerUx.errorSound(player);
            return;
        }
        if (!arenaService.isReady()) {
            PlayerUx.error(player, "Arena de duelo indisponível. Contacta staff.");
            PlayerUx.error(challenger, "Arena de duelo indisponível. Contacta staff.");
            return;
        }
        PlayerUx.success(player, "Duelo aceite! A preparar arena...");
        PlayerUx.info(challenger, player.getName() + " aceitou o teu desafio!");
        PlayerUx.confirmSound(player);
        PlayerUx.confirmSound(challenger);
        startDuel(challenger, player);
    }

    public void deny(Player player) {
        DuelInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null) {
            PlayerUx.error(player, "Não tens convites de duelo.");
            return;
        }
        PlayerUx.info(player, "Convite de duelo recusado.");
        Player challenger = Bukkit.getPlayer(invite.challenger());
        if (challenger != null && challenger.isOnline()) {
            PlayerUx.error(challenger, player.getName() + " recusou o teu duelo.");
            PlayerUx.errorSound(challenger);
        }
    }

    public void handleDeath(Player victim, Player killer) {
        Optional<Duel> duelOpt = getDuel(victim.getUniqueId());
        if (duelOpt.isEmpty() || duelOpt.get().state() != DuelState.ACTIVE) {
            return;
        }
        Duel duel = duelOpt.get();
        UUID roundWinnerId = killer != null ? killer.getUniqueId() : duel.opponent(victim.getUniqueId());
        if (roundWinnerId == null) {
            return;
        }

        duel.setState(DuelState.COUNTDOWN);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (victim.isOnline() && victim.isDead()) {
                victim.spigot().respawn();
            }
        });

        completeRound(duel, roundWinnerId, victim.getUniqueId());
    }

    public void handleDisconnect(Player player) {
        pendingInvites.remove(player.getUniqueId());
        Optional<Duel> duelOpt = getDuel(player.getUniqueId());
        if (duelOpt.isEmpty()) {
            return;
        }
        Duel duel = duelOpt.get();
        if (duel.state() == DuelState.ENDED) {
            return;
        }
        cancelCountdown(duel.id());
        UUID opponentId = duel.opponent(player.getUniqueId());
        if (opponentId != null) {
            stashLedgerForOffline(duel, player.getUniqueId());
            finishDuel(duel, opponentId, player.getUniqueId(), DuelEndReason.DISCONNECT);
        } else {
            cleanupDuel(duel);
        }
    }

    private void startDuel(Player playerOne, Player playerTwo) {
        Duel duel = new Duel(
                UUID.randomUUID().toString(),
                playerOne.getUniqueId(),
                playerTwo.getUniqueId(),
                DuelReturnPoint.capture(playerOne),
                DuelReturnPoint.capture(playerTwo),
                maxRounds
        );
        activeDuels.put(playerOne.getUniqueId(), duel);
        activeDuels.put(playerTwo.getUniqueId(), duel);

        CombatLogService.clearCombat(playerOne);
        CombatLogService.clearCombat(playerTwo);
        PlayerStateBridge.enterDuel(playerOne);
        PlayerStateBridge.enterDuel(playerTwo);

        DuelUx.sendDuelStart(playerOne);
        DuelUx.sendDuelStart(playerTwo);

        playerOne.sendMessage("§6Duelo Bo" + maxRounds + " contra §f" + playerTwo.getName() + "§6!");
        playerTwo.sendMessage("§6Duelo Bo" + maxRounds + " contra §f" + playerOne.getName() + "§6!");

        beginRound(duel, 1);
    }

    private void beginRound(Duel duel, int round) {
        if (duel.state() == DuelState.ENDED) {
            return;
        }

        Player playerOne = Bukkit.getPlayer(duel.playerOne());
        Player playerTwo = Bukkit.getPlayer(duel.playerTwo());
        if (playerOne == null || playerTwo == null || !playerOne.isOnline() || !playerTwo.isOnline()) {
            return;
        }

        cancelCountdown(duel.id());
        duel.setCurrentRound(round);
        duel.setState(DuelState.COUNTDOWN);

        resetRoundVitals(playerOne, playerTwo);

        Location s1 = arenaService.spawnOne();
        Location s2 = arenaService.spawnTwo();
        if (s1 != null) {
            playerOne.teleport(s1);
        }
        if (s2 != null) {
            playerTwo.teleport(s2);
        }

        String roundLabel = DuelUx.roundLabel(duel, round);
        DuelUx.sendRoundStart(playerOne, roundLabel);
        DuelUx.sendRoundStart(playerTwo, roundLabel);
        DuelUx.sendActionBar(playerOne, duel);
        DuelUx.sendActionBar(playerTwo, duel);

        runCountdown(duel, roundLabel);
    }

    private void runCountdown(Duel duel, String roundLabel) {
        cancelCountdown(duel.id());

        Player playerOne = Bukkit.getPlayer(duel.playerOne());
        Player playerTwo = Bukkit.getPlayer(duel.playerTwo());
        if (playerOne == null || playerTwo == null) {
            return;
        }

        final int[] remaining = {countdownSeconds};
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (duel.state() == DuelState.ENDED) {
                cancelCountdown(duel.id());
                return;
            }

            Player p1 = Bukkit.getPlayer(duel.playerOne());
            Player p2 = Bukkit.getPlayer(duel.playerTwo());
            if (p1 == null || p2 == null || !p1.isOnline() || !p2.isOnline()) {
                cancelCountdown(duel.id());
                return;
            }

            if (remaining[0] > 0) {
                DuelUx.sendCountdownTick(p1, remaining[0]);
                DuelUx.sendCountdownTick(p2, remaining[0]);
                remaining[0]--;
                return;
            }

            cancelCountdown(duel.id());
            duel.setState(DuelState.ACTIVE);
            duel.setRoundStartedAt(System.currentTimeMillis());
            DuelUx.sendFightStart(p1, roundLabel, duel);
            DuelUx.sendFightStart(p2, roundLabel, duel);
        }, 20L, 20L).getTaskId();

        countdownTasks.put(duel.id(), taskId);
    }

    private void cancelCountdown(String duelId) {
        Integer taskId = countdownTasks.remove(duelId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private void completeRound(Duel duel, UUID roundWinnerId, UUID roundLoserId) {
        duel.addRoundWin(roundWinnerId);

        Player winner = Bukkit.getPlayer(roundWinnerId);
        Player loser = Bukkit.getPlayer(roundLoserId);
        DuelUx.sendRoundWin(winner, loser, duel);

        if (duel.hasMatchWinner()) {
            UUID matchWinner = duel.matchWinner();
            UUID matchLoser = duel.opponent(matchWinner);
            finishDuel(duel, matchWinner, matchLoser, DuelEndReason.MATCH);
            return;
        }

        int nextRound = duel.currentRound() + 1;
        Bukkit.getScheduler().runTaskLater(plugin, () -> beginRound(duel, nextRound), 60L);
    }

    private void handleRoundTimeout(Duel duel) {
        if (duel.state() != DuelState.ACTIVE) {
            return;
        }

        Player playerOne = Bukkit.getPlayer(duel.playerOne());
        Player playerTwo = Bukkit.getPlayer(duel.playerTwo());
        if (playerOne == null || playerTwo == null) {
            return;
        }

        duel.setState(DuelState.COUNTDOWN);

        double healthOne = playerOne.getHealth();
        double healthTwo = playerTwo.getHealth();

        if (Math.abs(healthOne - healthTwo) < 0.01D) {
            playerOne.sendMessage("§eRound empatado por tempo — a recomeçar.");
            playerTwo.sendMessage("§eRound empatado por tempo — a recomeçar.");
            Bukkit.getScheduler().runTaskLater(plugin, () -> beginRound(duel, duel.currentRound()), 40L);
            return;
        }

        UUID roundWinnerId = healthOne > healthTwo ? duel.playerOne() : duel.playerTwo();
        UUID roundLoserId = duel.opponent(roundWinnerId);
        completeRound(duel, roundWinnerId, roundLoserId);
    }

    private void resetRoundVitals(Player... players) {
        for (Player player : players) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (player.isDead()) {
                player.spigot().respawn();
            }
            player.setHealth(player.getMaxHealth());
            player.setFireTicks(0);
            player.setNoDamageTicks(20);
        }
    }

    private void finishDuel(Duel duel, UUID winnerId, UUID loserId, DuelEndReason reason) {
        if (duel.state() == DuelState.ENDED) {
            return;
        }
        cancelCountdown(duel.id());
        duel.setState(DuelState.ENDED);

        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(loserId);

        if (reason == DuelEndReason.MATCH) {
            Bukkit.broadcastMessage("§6[Duel] §f" + nameOf(winnerId) + " §avenceu o duelo §7("
                    + duel.roundsWonOne() + "-" + duel.roundsWonTwo() + ")§a!");
            DuelUx.sendMatchVictory(winner, loser);
            if (winner != null && winner.isOnline()) {
                com.obm.network.core.integration.AchievementBridge.duelWin(winner);
            }
        } else if (reason == DuelEndReason.DISCONNECT) {
            if (winner != null && winner.isOnline()) {
                winner.sendMessage("§aVenceste — o adversário saiu do servidor.");
                DuelUx.sendMatchVictory(winner, loser);
            }
            Bukkit.broadcastMessage("§6[Duel] §f" + nameOf(winnerId) + " §avenceu (desconexão).");
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            releaseDuelPlayers(duel);
            cleanupDuel(duel);
        }, 40L);
    }

    private void finishTimeout(Duel duel) {
        if (duel.state() == DuelState.ENDED) {
            return;
        }
        cancelCountdown(duel.id());
        duel.setState(DuelState.ENDED);

        Player p1 = Bukkit.getPlayer(duel.playerOne());
        Player p2 = Bukkit.getPlayer(duel.playerTwo());
        if (p1 != null && p1.isOnline()) {
            p1.sendMessage("§eDuelo terminou por tempo — empate.");
        }
        if (p2 != null && p2.isOnline()) {
            p2.sendMessage("§eDuelo terminou por tempo — empate.");
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            releaseDuelPlayers(duel);
            cleanupDuel(duel);
        }, 40L);
    }

    private void forceEnd(Duel duel, DuelEndReason reason) {
        if (duel.state() == DuelState.ENDED) {
            return;
        }
        cancelCountdown(duel.id());
        duel.setState(DuelState.ENDED);
        stashOfflineLedgers(duel);
        releaseDuelPlayers(duel);
        cleanupDuel(duel);
    }

    private void releaseDuelPlayers(Duel duel) {
        releasePlayer(Bukkit.getPlayer(duel.playerOne()), duel);
        releasePlayer(Bukkit.getPlayer(duel.playerTwo()), duel);
    }

    private void releasePlayer(Player player, Duel duel) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!player.isOnline()) {
            stashLedgerForOffline(duel, uuid);
            return;
        }

        if (player.isDead()) {
            player.spigot().respawn();
        }

        PlayerStateBridge.exitDuel(player);
        CombatLogService.clearCombat(player);

        DuelReturnPoint returnPoint = duel.returnFor(uuid);
        if (returnToOriginal && returnPoint != null && returnPoint.location() != null) {
            Location target = returnPoint.location();
            World world = target.getWorld();
            if (world != null && worldModeService.isSMP(world.getName())) {
                Location safe = SafeSpawnService.getSafeSpawn(world, target);
                player.teleport(safe != null ? safe : target);
            } else {
                teleportToSmpSpawn(player);
            }
        } else {
            teleportToSmpSpawn(player);
        }

        deliverLedger(player, duel.ledgerFor(uuid));
        player.setFireTicks(0);
        player.setNoDamageTicks(40);
    }

    private void deliverLedger(Player player, DuelItemLedger ledger) {
        if (player == null || !player.isOnline() || ledger == null) {
            return;
        }
        for (ItemStack broken : ledger.drainBroken()) {
            ItemStack restored = DuelItemUtil.asLowDurabilityReturn(broken);
            if (restored != null) {
                DuelItemUtil.giveSafely(player, restored);
                player.sendMessage("§aRelaxa! O teu item foi devolvido com pouca durabilidade.");
            }
        }
        for (ItemStack drop : ledger.drainDrops()) {
            DuelItemUtil.giveSafely(player, drop);
        }
    }

    private void deliverPendingReturns(Player player) {
        List<ItemStack> pending = pendingStore.drain(player.getUniqueId());
        if (pending.isEmpty()) {
            return;
        }
        boolean hadBroken = false;
        for (ItemStack item : pending) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (isLowDurabilityReturn(item)) {
                hadBroken = true;
            }
            DuelItemUtil.giveSafely(player, item);
        }
        if (hadBroken) {
            player.sendMessage("§aRelaxa! O teu item foi devolvido com pouca durabilidade.");
        }
        player.sendMessage("§7Itens do duelo foram devolvidos ao teu inventário.");
    }

    private boolean isLowDurabilityReturn(ItemStack item) {
        int max = item.getType().getMaxDurability();
        if (max <= 0) {
            return false;
        }
        var meta = item.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
            return damageable.getDamage() >= (int) Math.floor(max * 0.90);
        }
        return false;
    }

    private void stashLedgerForOffline(Duel duel, UUID playerId) {
        DuelItemLedger ledger = duel.ledgerFor(playerId);
        if (ledger == null || ledger.isEmpty()) {
            return;
        }
        java.util.ArrayList<ItemStack> payload = new java.util.ArrayList<>();
        for (ItemStack broken : ledger.drainBroken()) {
            ItemStack restored = DuelItemUtil.asLowDurabilityReturn(broken);
            if (restored != null) {
                payload.add(restored);
            }
        }
        payload.addAll(ledger.drainDrops());
        pendingStore.add(playerId, payload);
    }

    private void stashOfflineLedgers(Duel duel) {
        stashLedgerForOffline(duel, duel.playerOne());
        stashLedgerForOffline(duel, duel.playerTwo());
    }

    private void teleportToSmpSpawn(Player player) {
        for (World world : Bukkit.getWorlds()) {
            if (worldModeService.isSMP(world.getName())) {
                Location spawn = world.getSpawnLocation();
                Location safe = SafeSpawnService.getSafeSpawn(world, spawn);
                player.teleport(safe != null ? safe : spawn);
                return;
            }
        }
    }

    private void cleanupDuel(Duel duel) {
        cancelCountdown(duel.id());
        activeDuels.remove(duel.playerOne());
        activeDuels.remove(duel.playerTwo());
    }

    private void tick() {
        long now = System.currentTimeMillis();
        pendingInvites.entrySet().removeIf(entry -> {
            if (!entry.getValue().isExpired()) {
                return false;
            }
            Player challenged = Bukkit.getPlayer(entry.getKey());
            if (challenged != null && challenged.isOnline()) {
                challenged.sendMessage("§7Convite de duelo expirou.");
            }
            Player challenger = Bukkit.getPlayer(entry.getValue().challenger());
            if (challenger != null && challenger.isOnline()) {
                challenger.sendMessage("§7O teu convite de duelo expirou.");
            }
            return true;
        });

        for (Duel duel : uniqueActiveDuels()) {
            if (duel.state() == DuelState.ENDED) {
                continue;
            }
            if (now - duel.startedAt() >= maxDurationMs) {
                finishTimeout(duel);
                continue;
            }
            if (duel.state() == DuelState.ACTIVE && duel.roundStartedAt() > 0L
                    && now - duel.roundStartedAt() >= roundTimeoutMs) {
                handleRoundTimeout(duel);
                continue;
            }
            Player p1 = Bukkit.getPlayer(duel.playerOne());
            Player p2 = Bukkit.getPlayer(duel.playerTwo());
            if (p1 == null && p2 == null) {
                stashOfflineLedgers(duel);
                cleanupDuel(duel);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (PlayerStateBridge.isInDuel(player) && !isInDuel(player.getUniqueId())) {
                PlayerStateBridge.exitDuel(player);
                if (arenaService.isDuelWorld(player.getWorld().getName())) {
                    teleportToSmpSpawn(player);
                }
            }
        }
    }

    private void reconcileOrphans() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (arenaService.isDuelWorld(player.getWorld().getName()) || PlayerStateBridge.isInDuel(player)) {
                PlayerStateBridge.exitDuel(player);
                teleportToSmpSpawn(player);
                deliverPendingReturns(player);
                player.sendMessage("§eDuelo interrompido (restart). Foste enviado ao spawn SMP.");
            }
        }
    }

    private Iterable<Duel> uniqueActiveDuels() {
        return activeDuels.values().stream().distinct().toList();
    }

    private String duelBlockReason(Player player) {
        if (player == null || !player.isOnline()) {
            return "§cJogador offline.";
        }
        if (isInDuel(player.getUniqueId()) || PlayerStateBridge.isInDuel(player)) {
            return "§cJá estás num duelo.";
        }
        if (PlayerStateBridge.isInQueue(player)) {
            return "§cNão podes duelar na queue.";
        }
        if (CombatLogService.isTaggedInSmpCombat(player) || PlayerStateBridge.isInCombat(player)) {
            return "§cNão podes duelar em combate.";
        }
        return null;
    }

    private static String nameOf(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getName() : uuid.toString().substring(0, 8);
    }
}

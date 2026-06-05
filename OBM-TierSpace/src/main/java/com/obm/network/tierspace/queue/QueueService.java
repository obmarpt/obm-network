package com.obm.network.tierspace.queue;

import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.storage.TierSpaceStore;
import com.obm.network.tierspace.util.TierSpaceLog;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QueueService {

    public record QueueEntry(UUID uuid, GameModeId mode, int rating, long joinedAt) {
        public QueuePlayer toPlayer() {
            return QueuePlayer.fromEntry(this);
        }
    }

    public record MatchPair(QueueEntry first, QueueEntry second) {
    }

    private final TierSpaceStore store;
    private final int initialRange;
    private final int rangeExpansion;
    private final int expansionIntervalSeconds;
    private final int maxRange;

    private final Map<UUID, QueueEntry> queuedPlayers = new ConcurrentHashMap<>();

    public QueueService(TierSpaceStore store, int initialRange, int rangeExpansion,
                        int expansionIntervalSeconds, int maxRange) {
        this.store = store;
        this.initialRange = initialRange;
        this.rangeExpansion = rangeExpansion;
        this.expansionIntervalSeconds = Math.max(1, expansionIntervalSeconds);
        this.maxRange = maxRange;
    }

    public enum JoinResult {
        JOINED,
        SWITCHED,
        REFRESHED
    }

    public JoinResult joinOrSwitch(Player player, GameModeId mode) {
        UUID uuid = player.getUniqueId();
        QueueEntry existing = queuedPlayers.get(uuid);
        if (existing != null) {
            if (existing.mode() == mode) {
                queuedPlayers.put(uuid, new QueueEntry(uuid, mode, existing.rating(),
                    System.currentTimeMillis() - queuePriorityBoostMs(player)));
                TierSpaceLog.debug("Queue refresh " + player.getName() + " mode=" + mode.id());
                return JoinResult.REFRESHED;
            }
            queuedPlayers.remove(uuid);
            store.ensureInitialized(uuid, mode);
            int rating = store.getRating(uuid, mode);
            queuedPlayers.put(uuid, new QueueEntry(uuid, mode, rating,
                    System.currentTimeMillis() - queuePriorityBoostMs(player)));
            TierSpaceLog.info("Queue switch " + player.getName() + " mode=" + mode.id());
            return JoinResult.SWITCHED;
        }
        store.ensureInitialized(uuid, mode);
        int rating = store.getRating(uuid, mode);
        long joinedAt = System.currentTimeMillis() - queuePriorityBoostMs(player);
        queuedPlayers.put(uuid, new QueueEntry(uuid, mode, rating, joinedAt));
        TierSpaceLog.info("Queue join " + player.getName() + " mode=" + mode.id() + " rating=" + rating);
        return JoinResult.JOINED;
    }

    public boolean join(Player player, GameModeId mode) {
        JoinResult result = joinOrSwitch(player, mode);
        return result == JoinResult.JOINED || result == JoinResult.SWITCHED || result == JoinResult.REFRESHED;
    }

    public Optional<GameModeId> getQueuedMode(UUID uuid) {
        return getEntry(uuid).map(QueueEntry::mode);
    }

    public int getQueueSize(String modeId) {
        return GameModeId.from(modeId).map(this::getQueueSize).orElse(0);
    }

    public boolean leave(UUID uuid) {
        boolean removed = queuedPlayers.remove(uuid) != null;
        if (removed) {
            TierSpaceLog.debug("Queue leave uuid=" + uuid);
        }
        return removed;
    }

    public boolean isQueued(UUID uuid) {
        return queuedPlayers.containsKey(uuid);
    }

    public Optional<QueueEntry> getEntry(UUID uuid) {
        return Optional.ofNullable(queuedPlayers.get(uuid));
    }

    public Optional<QueuePlayer> getPlayer(UUID uuid) {
        return getEntry(uuid).map(QueueEntry::toPlayer);
    }

    public List<QueuePlayer> getPlayersInMode(GameModeId mode) {
        return queuedPlayers.values().stream()
                .filter(e -> e.mode() == mode)
                .map(QueueEntry::toPlayer)
                .toList();
    }

    /**
     * Matchmaking ELO — filas independentes por modo, range expande com o tempo.
     */
    public Optional<MatchPair> findMatch() {
        for (GameModeId mode : GameModeId.ordered()) {
            Optional<MatchPair> pair = findMatchForMode(mode);
            if (pair.isPresent()) {
                return pair;
            }
        }
        return Optional.empty();
    }

    private Optional<MatchPair> findMatchForMode(GameModeId mode) {
        List<QueueEntry> entries = queuedPlayers.values().stream()
                .filter(e -> e.mode() == mode)
                .sorted(Comparator.comparingLong(QueueEntry::joinedAt))
                .toList();

        if (entries.size() < 2) {
            return Optional.empty();
        }

        MatchPair best = null;
        int bestDiff = Integer.MAX_VALUE;

        for (int i = 0; i < entries.size(); i++) {
            QueueEntry anchor = entries.get(i);
            int allowedRange = currentRange(anchor);

            for (int j = i + 1; j < entries.size(); j++) {
                QueueEntry candidate = entries.get(j);
                int diff = Math.abs(anchor.rating() - candidate.rating());
                if (diff <= allowedRange && diff < bestDiff) {
                    bestDiff = diff;
                    best = new MatchPair(anchor, candidate);
                }
            }
        }

        if (best == null) {
            return Optional.empty();
        }

        queuedPlayers.remove(best.first().uuid());
        queuedPlayers.remove(best.second().uuid());
        TierSpaceLog.info("Match pair mode=" + mode.id()
                + " ratings=" + best.first().rating() + "/" + best.second().rating()
                + " diff=" + bestDiff);
        return Optional.of(best);
    }

    /**
     * Range: ±50 inicial → ±100 → ±200 → cresce até maxRange a cada intervalo.
     */
    int currentRange(QueueEntry entry) {
        long waitedSeconds = Math.max(0, (System.currentTimeMillis() - entry.joinedAt()) / 1000L);
        int expansions = (int) (waitedSeconds / expansionIntervalSeconds);
        int range = initialRange;
        for (int i = 0; i < expansions; i++) {
            if (range < 100) {
                range = 100;
            } else if (range < 200) {
                range = 200;
            } else {
                range = Math.min(maxRange, range + rangeExpansion);
            }
        }
        return Math.min(maxRange, range);
    }

    public int getQueueSize(GameModeId mode) {
        return (int) queuedPlayers.values().stream().filter(entry -> entry.mode() == mode).count();
    }

    public long getWaitSeconds(UUID uuid) {
        QueueEntry entry = queuedPlayers.get(uuid);
        if (entry == null) {
            return 0;
        }
        return Math.max(0, (System.currentTimeMillis() - entry.joinedAt()) / 1000L);
    }

    public int estimateWaitSeconds(GameModeId mode, UUID uuid) {
        int inQueue = getQueueSize(mode);
        if (inQueue <= 1) {
            return 15;
        }
        long waited = getWaitSeconds(uuid);
        return (int) Math.min(90, Math.max(10, inQueue * 6L - waited / 2L));
    }

    private static long queuePriorityBoostMs(Player player) {
        if (player.hasPermission("group.mvp_plus")) {
            return 120_000L;
        }
        if (player.hasPermission("group.mvp") || player.hasPermission("obm.queue.priority")) {
            return 60_000L;
        }
        return 0L;
    }
}

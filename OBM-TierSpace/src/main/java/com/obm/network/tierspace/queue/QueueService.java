package com.obm.network.tierspace.queue;

import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.storage.TierSpaceStore;
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
        this.expansionIntervalSeconds = expansionIntervalSeconds;
        this.maxRange = maxRange;
    }

    public boolean join(Player player, GameModeId mode) {
        UUID uuid = player.getUniqueId();
        if (queuedPlayers.containsKey(uuid)) {
            return false;
        }
        store.ensureInitialized(uuid, mode);
        queuedPlayers.put(uuid, new QueueEntry(uuid, mode, store.getRating(uuid, mode), System.currentTimeMillis()));
        return true;
    }

    public boolean leave(UUID uuid) {
        return queuedPlayers.remove(uuid) != null;
    }

    public boolean isQueued(UUID uuid) {
        return queuedPlayers.containsKey(uuid);
    }

    public Optional<QueueEntry> getEntry(UUID uuid) {
        return Optional.ofNullable(queuedPlayers.get(uuid));
    }

    public Optional<MatchPair> findMatch() {
        List<QueueEntry> entries = new ArrayList<>(queuedPlayers.values());
        entries.sort(Comparator.comparingLong(QueueEntry::joinedAt));

        for (int i = 0; i < entries.size(); i++) {
            QueueEntry anchor = entries.get(i);
            int allowedRange = currentRange(anchor);

            for (int j = i + 1; j < entries.size(); j++) {
                QueueEntry candidate = entries.get(j);
                if (anchor.mode() != candidate.mode()) {
                    continue;
                }
                if (Math.abs(anchor.rating() - candidate.rating()) <= allowedRange) {
                    queuedPlayers.remove(anchor.uuid());
                    queuedPlayers.remove(candidate.uuid());
                    return Optional.of(new MatchPair(anchor, candidate));
                }
            }
        }
        return Optional.empty();
    }

    private int currentRange(QueueEntry entry) {
        long waitedSeconds = Math.max(0, (System.currentTimeMillis() - entry.joinedAt()) / 1000L);
        int expansions = (int) (waitedSeconds / Math.max(1, expansionIntervalSeconds));
        return Math.min(maxRange, initialRange + expansions * rangeExpansion);
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
        return (int) Math.min(60, Math.max(10, inQueue * 8L - waited / 2L));
    }
}

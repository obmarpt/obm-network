package com.obm.network.tierspace.queue;

import com.obm.network.tierspace.TierSpacePlugin;
import com.obm.network.tierspace.feedback.MatchFeedbackService;
import com.obm.network.tierspace.storage.TierSpaceStore;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QueueFeedbackService {

    private final TierSpacePlugin plugin;
    private final QueueService queueService;
    private final TierSpaceStore store;
    private final MatchFeedbackService feedbackService;

    private final Map<UUID, Long> lastSoundAt = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public QueueFeedbackService(TierSpacePlugin plugin,
                                QueueService queueService,
                                TierSpaceStore store,
                                MatchFeedbackService feedbackService) {
        this.plugin = plugin;
        this.queueService = queueService;
        this.store = store;
        this.feedbackService = feedbackService;
    }

    public void start() {
        if (updateTask != null) {
            return;
        }
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 40L);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        lastSoundAt.clear();
    }

    public void startTracking(Player player) {
        lastSoundAt.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void stopTracking(UUID uuid) {
        lastSoundAt.remove(uuid);
    }

    private void tick() {
        for (UUID uuid : lastSoundAt.keySet()) {
            if (!queueService.isQueued(uuid)) {
                stopTracking(uuid);
                continue;
            }

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                stopTracking(uuid);
                continue;
            }

            queueService.getEntry(uuid).ifPresent(entry -> {
                int queueSize = queueService.getQueueSize(entry.mode());
                int rating = store.getRating(uuid, entry.mode());
                long waited = queueService.getWaitSeconds(uuid);
                int estimate = queueService.estimateWaitSeconds(entry.mode(), uuid);

                feedbackService.sendQueueStatus(
                        player,
                        entry.mode().displayName(),
                        queueSize,
                        estimate,
                        rating,
                        waited
                );

                maybePlayQueueSound(player, uuid);
            });
        }
    }

    private void maybePlayQueueSound(Player player, UUID uuid) {
        long now = System.currentTimeMillis();
        long last = lastSoundAt.getOrDefault(uuid, now);
        if (now - last >= 10_000L) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.35f, 1.6f);
            lastSoundAt.put(uuid, now);
        }
    }
}

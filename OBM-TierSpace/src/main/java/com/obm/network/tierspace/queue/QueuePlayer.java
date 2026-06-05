package com.obm.network.tierspace.queue;

import com.obm.network.tierspace.mode.GameModeId;

import java.util.UUID;

/**
 * Jogador na fila competitiva — rating ELO + tempo de espera.
 */
public record QueuePlayer(UUID uuid, GameModeId mode, int rating, long timeInQueue) {

    public long waitSeconds() {
        return Math.max(0L, (System.currentTimeMillis() - timeInQueue) / 1000L);
    }

    public QueueService.QueueEntry toEntry() {
        return new QueueService.QueueEntry(uuid, mode, rating, timeInQueue);
    }

    public static QueuePlayer fromEntry(QueueService.QueueEntry entry) {
        return new QueuePlayer(entry.uuid(), entry.mode(), entry.rating(), entry.joinedAt());
    }
}

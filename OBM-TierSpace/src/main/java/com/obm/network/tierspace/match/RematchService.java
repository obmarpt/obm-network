package com.obm.network.tierspace.match;

import com.obm.network.tierspace.mode.GameModeId;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RematchService {

    private static final long REMATCH_WINDOW_MS = 60_000L;

    private final Map<UUID, LastMatch> lastMatches = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> rematchReady = new ConcurrentHashMap<>();

    public void registerFinishedMatch(UUID playerOne, UUID playerTwo, GameModeId mode) {
        long now = System.currentTimeMillis();
        String nameOne = resolveName(playerOne);
        String nameTwo = resolveName(playerTwo);

        lastMatches.put(playerOne, new LastMatch(playerTwo, nameTwo, mode, now));
        lastMatches.put(playerTwo, new LastMatch(playerOne, nameOne, mode, now));
        rematchReady.remove(playerOne);
        rematchReady.remove(playerTwo);
    }

    public Optional<LastMatch> getLastMatch(UUID uuid) {
        LastMatch match = lastMatches.get(uuid);
        if (match == null || System.currentTimeMillis() - match.finishedAt() > REMATCH_WINDOW_MS) {
            return Optional.empty();
        }
        return Optional.of(match);
    }

    public RematchResult requestRematch(Player player) {
        Optional<LastMatch> lastOptional = getLastMatch(player.getUniqueId());
        if (lastOptional.isEmpty()) {
            return RematchResult.unavailable();
        }

        LastMatch last = lastOptional.get();
        Player opponent = Bukkit.getPlayer(last.opponentId());
        if (opponent == null || !opponent.isOnline()) {
            return RematchResult.unavailable();
        }

        UUID opponentReadyWith = rematchReady.get(last.opponentId());
        if (opponentReadyWith != null && opponentReadyWith.equals(player.getUniqueId())) {
            rematchReady.remove(player.getUniqueId());
            rematchReady.remove(last.opponentId());
            return RematchResult.ready(last.opponentId(), last.mode());
        }

        rematchReady.put(player.getUniqueId(), last.opponentId());
        return RematchResult.waiting(last.opponentName());
    }

    public void clear(UUID uuid) {
        lastMatches.remove(uuid);
        rematchReady.remove(uuid);
    }

    private String resolveName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        String offline = Bukkit.getOfflinePlayer(uuid).getName();
        return offline == null ? "oponente" : offline;
    }

    public record LastMatch(UUID opponentId, String opponentName, GameModeId mode, long finishedAt) {
    }

    public record RematchResult(Status status, UUID opponentId, GameModeId mode, String opponentName) {
        public enum Status {
            WAITING,
            READY,
            UNAVAILABLE
        }

        static RematchResult waiting(String opponentName) {
            return new RematchResult(Status.WAITING, null, null, opponentName);
        }

        static RematchResult ready(UUID opponentId, GameModeId mode) {
            return new RematchResult(Status.READY, opponentId, mode, null);
        }

        static RematchResult unavailable() {
            return new RematchResult(Status.UNAVAILABLE, null, null, null);
        }
    }
}

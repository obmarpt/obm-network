package com.obm.network.tierspace.arena;

import com.obm.network.tierspace.mode.GameModeId;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArenaSetupManager {

    public record PendingArena(String id, String name, String mode, Location pos1, Location pos2) {
        boolean readyToSave() {
            return name != null && !name.isBlank() && pos1 != null && pos2 != null && mode != null;
        }
    }

    private final Map<UUID, PendingArena> sessions = new ConcurrentHashMap<>();

    public void startCreate(Player player, String name, String modeId) {
        String slug = slugify(name);
        String id = slug + "_" + System.currentTimeMillis() % 100000;
        String mode = GameModeId.from(modeId).map(GameModeId::id).orElse("sword");
        sessions.put(player.getUniqueId(), new PendingArena(id, name, mode, null, null));
    }

    public boolean setPos1(Player player) {
        return update(player, sessions.get(player.getUniqueId()), player.getLocation(), null);
    }

    public boolean setPos2(Player player) {
        return update(player, sessions.get(player.getUniqueId()), null, player.getLocation());
    }

    public boolean setMode(Player player, String modeId) {
        PendingArena pending = sessions.get(player.getUniqueId());
        if (pending == null) {
            return false;
        }
        String mode = GameModeId.from(modeId).map(GameModeId::id).orElse(null);
        if (mode == null) {
            return false;
        }
        sessions.put(player.getUniqueId(), new PendingArena(
                pending.id(), pending.name(), mode, pending.pos1(), pending.pos2()));
        return true;
    }

    public PendingArena get(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void clear(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public ArenaDefinition build(PendingArena pending) {
        if (pending == null || !pending.readyToSave()) {
            return null;
        }
        return ArenaDefinition.fromBackend(
                pending.id(),
                pending.name(),
                pending.mode(),
                pending.pos1().getWorld().getName(),
                pending.pos1().clone(),
                pending.pos2().clone(),
                true,
                false);
    }

    private boolean update(Player player, PendingArena pending, Location pos1, Location pos2) {
        if (pending == null) {
            return false;
        }
        sessions.put(player.getUniqueId(), new PendingArena(
                pending.id(),
                pending.name(),
                pending.mode(),
                pos1 != null ? pos1.clone() : pending.pos1(),
                pos2 != null ? pos2.clone() : pending.pos2()));
        return true;
    }

    private static String slugify(String name) {
        if (name == null) {
            return "arena";
        }
        String s = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        if (s.length() > 40) {
            s = s.substring(0, 40);
        }
        return s.isBlank() ? "arena" : s;
    }
}

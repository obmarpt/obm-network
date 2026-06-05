package com.obm.network.tierspace.arena;

import com.obm.network.tierspace.mode.GameModeId;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public class ArenaService {

    private final List<ArenaDefinition> arenas = new ArrayList<>();
    private final Set<String> occupied = new HashSet<>();
    private final Logger logger;

    public ArenaService(Logger logger) {
        this.logger = logger;
    }

    public void reload(FileConfiguration config) {
        List<ArenaDefinition> fromConfig = new ArrayList<>();
        String worldName = config.getString("world", "TierSpace");
        ConfigurationSection section = config.getConfigurationSection("arenas");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ArenaDefinition arena = ArenaDefinition.fromConfig(id, section.getConfigurationSection(id), worldName);
                if (arena != null) {
                    fromConfig.add(arena);
                }
            }
        }
        replaceAll(fromConfig);
    }

    public synchronized void replaceAll(List<ArenaDefinition> loaded) {
        occupied.clear();
        arenas.clear();
        if (loaded != null) {
            arenas.addAll(loaded);
        }
        logger.info("[Arena] Carregadas " + arenas.size() + " arenas.");
    }

    public synchronized void mergeBackend(List<ArenaDefinition> backendArenas) {
        if (backendArenas == null || backendArenas.isEmpty()) {
            return;
        }
        List<String> backendIds = backendArenas.stream().map(ArenaDefinition::id).toList();
        arenas.removeIf(a -> backendIds.contains(a.id()));
        arenas.addAll(backendArenas);
        logger.info("[Arena] Sync backend: " + backendArenas.size() + " arenas (total " + arenas.size() + ").");
    }

    public synchronized Optional<ArenaDefinition> acquireArena(GameModeId mode) {
        for (ArenaDefinition arena : arenas) {
            if (arena.supports(mode.id()) && !occupied.contains(arena.id())) {
                occupied.add(arena.id());
                return Optional.of(arena);
            }
        }
        return Optional.empty();
    }

    public synchronized void releaseArena(String arenaId) {
        if (arenaId == null) {
            return;
        }
        occupied.remove(arenaId);
        findArena(arenaId).ifPresent(ArenaDefinition::resetBlocks);
    }

    public Optional<ArenaDefinition> findArena(String arenaId) {
        return arenas.stream().filter(arena -> arena.id().equals(arenaId)).findFirst();
    }

    public List<ArenaDefinition> listArenas() {
        return List.copyOf(arenas);
    }

    public int getArenaCount() {
        return arenas.size();
    }

    public int getArenaCount(GameModeId mode) {
        return (int) arenas.stream()
                .filter(a -> a.enabled() && a.supports(mode.id()))
                .count();
    }

    public int getAvailableCount(GameModeId mode) {
        return (int) arenas.stream()
                .filter(a -> a.enabled() && a.supports(mode.id()) && !occupied.contains(a.id()))
                .count();
    }

    public int getOccupiedCount() {
        return occupied.size();
    }

    public boolean isOccupied(String arenaId) {
        return occupied.contains(arenaId);
    }
}

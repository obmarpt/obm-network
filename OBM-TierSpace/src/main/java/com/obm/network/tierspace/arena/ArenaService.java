package com.obm.network.tierspace.arena;

import com.obm.network.tierspace.mode.GameModeId;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ArenaService {

    private final List<ArenaDefinition> arenas = new ArrayList<>();
    private final Set<String> occupied = new HashSet<>();

    public void reload(FileConfiguration config) {
        arenas.clear();
        occupied.clear();

        String worldName = config.getString("world", "TierSpace");
        ConfigurationSection section = config.getConfigurationSection("arenas");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ArenaDefinition arena = ArenaDefinition.fromConfig(id, section.getConfigurationSection(id), worldName);
            if (arena != null) {
                arenas.add(arena);
            }
        }
    }

    public Optional<ArenaDefinition> acquireArena(GameModeId mode) {
        for (ArenaDefinition arena : arenas) {
            if (arena.supports(mode.id()) && !occupied.contains(arena.id())) {
                occupied.add(arena.id());
                return Optional.of(arena);
            }
        }
        return Optional.empty();
    }

    public void releaseArena(String arenaId) {
        if (arenaId == null) {
            return;
        }
        occupied.remove(arenaId);
        findArena(arenaId).ifPresent(ArenaDefinition::resetBlocks);
    }

    public Optional<ArenaDefinition> findArena(String arenaId) {
        return arenas.stream().filter(arena -> arena.id().equals(arenaId)).findFirst();
    }

    public int getArenaCount() {
        return arenas.size();
    }

    public int getArenaCount(GameModeId mode) {
        return (int) arenas.stream().filter(arena -> arena.supports(mode.id())).count();
    }
}

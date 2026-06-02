package com.obm.network.tierspace.mode;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModeRegistry {

    private final Map<GameModeId, ModeConfig> configs = new EnumMap<>(GameModeId.class);

    public void reload(FileConfiguration config) {
        configs.clear();
        for (GameModeId mode : GameModeId.values()) {
            ConfigurationSection section = config.getConfigurationSection("modes." + mode.id());
            boolean enabled = section != null && section.getBoolean("enabled", false);
            String displayName = section != null
                    ? section.getString("display-name", mode.defaultDisplayName())
                    : mode.defaultDisplayName();
            String kit = section != null ? section.getString("kit", mode.id()) : mode.id();
            int guiSlot = section != null ? section.getInt("gui-slot", mode.defaultGuiSlot()) : mode.defaultGuiSlot();
            configs.put(mode, new ModeConfig(enabled, displayName, kit, guiSlot));
        }
    }

    public boolean isEnabled(GameModeId mode) {
        return configs.getOrDefault(mode, ModeConfig.disabled(mode)).enabled();
    }

    public String displayName(GameModeId mode) {
        return configs.getOrDefault(mode, ModeConfig.disabled(mode)).displayName();
    }

    public String kitId(GameModeId mode) {
        return configs.getOrDefault(mode, ModeConfig.disabled(mode)).kitId();
    }

    public int guiSlot(GameModeId mode) {
        return configs.getOrDefault(mode, ModeConfig.disabled(mode)).guiSlot();
    }

    public List<GameModeId> enabledModes() {
        List<GameModeId> enabled = new ArrayList<>();
        for (GameModeId mode : GameModeId.ordered()) {
            if (isEnabled(mode)) {
                enabled.add(mode);
            }
        }
        return enabled;
    }

    public record ModeConfig(boolean enabled, String displayName, String kitId, int guiSlot) {
        static ModeConfig disabled(GameModeId mode) {
            return new ModeConfig(false, mode.defaultDisplayName(), mode.id(), mode.defaultGuiSlot());
        }
    }
}

package com.obm.network.smp.progression;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ranks de progressão gameplay SMP (Rookie → Godlike). Separado dos ranks network (OBM-Core).
 */
public class RankCatalog {

    private static final Map<String, String> LEGACY_RANK_IDS = Map.of(
            "bronze", "rookie",
            "silver", "survivor",
            "gold", "fighter",
            "diamond", "warrior",
            "emerald", "knight"
    );

    private final List<RankDefinition> ranks = new ArrayList<>();
    private final Map<String, RankDefinition> rankById = new LinkedHashMap<>();
    private String defaultRankId = "rookie";

    public void reload(FileConfiguration config) {
        ranks.clear();
        rankById.clear();
        defaultRankId = config.getString("ranks.default", "rookie");

        ConfigurationSection section = config.getConfigurationSection("ranks.levels");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection rankSection = section.getConfigurationSection(id);
            if (rankSection == null) {
                continue;
            }
            RankDefinition rank = new RankDefinition(
                    id,
                    rankSection.getString("display-name", id),
                    rankSection.getInt("cost", 0),
                    rankSection.getDouble("money-boost", 0),
                    rankSection.getDouble("sell-boost", 0),
                    rankSection.getDouble("kill-boost", 0),
                    rankSection.getDouble("shop-discount", 0),
                    rankSection.getInt("extra-homes", 1),
                    rankSection.getDouble("cooldown-reduction", 0),
                    rankSection.getString("chat-prefix", "")
            );
            ranks.add(rank);
            rankById.put(id, rank);
        }
    }

    public String normalizeRankId(String rankId) {
        if (rankId == null || rankId.isBlank()) {
            return defaultRankId;
        }
        String normalized = LEGACY_RANK_IDS.getOrDefault(rankId.toLowerCase(), rankId.toLowerCase());
        if (rankById.containsKey(normalized)) {
            return normalized;
        }
        return defaultRankId;
    }

    public String getDefaultRankId() {
        return defaultRankId;
    }

    public List<RankDefinition> getRanks() {
        return Collections.unmodifiableList(ranks);
    }

    public Optional<RankDefinition> getRank(String id) {
        return Optional.ofNullable(rankById.get(normalizeRankId(id)));
    }

    public Optional<RankDefinition> getNextRank(String currentRankId) {
        String normalized = normalizeRankId(currentRankId);
        int index = indexOf(normalized);
        if (index < 0 || index + 1 >= ranks.size()) {
            return Optional.empty();
        }
        return Optional.of(ranks.get(index + 1));
    }

    public int indexOf(String rankId) {
        String normalized = normalizeRankId(rankId);
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).id().equals(normalized)) {
                return i;
            }
        }
        return -1;
    }
}

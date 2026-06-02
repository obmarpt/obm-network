package com.obm.network.smp.progression;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LevelService {

    public record LevelUpResult(boolean leveledUp, int newLevel, int coinsReward, String message) {}

    private final PlayerProgressionStore store;
    private final EconomyServiceAdapter economy;

    private int xpPerKill;
    private double xpPerSellCoin;
    private int xpPerPlaytimeInterval;
    private int baseXpRequired;
    private double xpScale;
    private int maxLevel;
    private int rewardPerLevel;
    private double levelSellBonusPerLevel;
    private double levelKillBonusPerLevel;

    public LevelService(PlayerProgressionStore store, EconomyServiceAdapter economy) {
        this.store = store;
        this.economy = economy;
    }

    public void reload(FileConfiguration config) {
        this.xpPerKill = config.getInt("levels.xp-per-kill", 20);
        this.xpPerSellCoin = config.getDouble("levels.xp-per-sell-coin", 0.5);
        this.xpPerPlaytimeInterval = config.getInt("levels.xp-per-playtime-interval", 15);
        this.baseXpRequired = config.getInt("levels.base-xp-required", 100);
        this.xpScale = config.getDouble("levels.xp-scale", 1.12);
        this.maxLevel = config.getInt("levels.max-level", 100);
        this.rewardPerLevel = config.getInt("levels.reward-per-level-coins", 25);
        this.levelSellBonusPerLevel = config.getDouble("levels.bonus-per-level.sell", 0.001);
        this.levelKillBonusPerLevel = config.getDouble("levels.bonus-per-level.kill", 0.001);
    }

    public int getLevel(UUID uuid) {
        return store.getLevel(uuid);
    }

    public int getXp(UUID uuid) {
        return store.getXp(uuid);
    }

    public int getXpRequired(int level) {
        if (level >= maxLevel) return Integer.MAX_VALUE;
        return (int) Math.ceil(baseXpRequired * Math.pow(xpScale, level - 1));
    }

    public double getLevelSellMultiplier(UUID uuid) {
        return 1.0 + (getLevel(uuid) - 1) * levelSellBonusPerLevel;
    }

    public double getLevelKillMultiplier(UUID uuid) {
        return 1.0 + (getLevel(uuid) - 1) * levelKillBonusPerLevel;
    }

    /*
     * ✅ XP SOURCES
     */
    public LevelUpResult addKillXp(Player player) {
        return addXp(player, xpPerKill, "kill");
    }

    public LevelUpResult addSellXp(Player player, int coinsEarned) {
        int xp = (int) Math.max(1, Math.floor(coinsEarned * xpPerSellCoin));
        return addXp(player, xp, "sell");
    }

    public LevelUpResult addPlaytimeXp(Player player) {
        return addXp(player, xpPerPlaytimeInterval, "playtime");
    }

    /*
     * ✅ CORE SYSTEM
     */
    private LevelUpResult addXp(Player player, int amount, String source) {

        UUID uuid = player.getUniqueId();
        int level = getLevel(uuid);

        if (level >= maxLevel) {
            return new LevelUpResult(false, level, 0, null);
        }

        int xp = getXp(uuid) + amount;
        store.setXp(uuid, xp);

        int totalCoins = 0;
        boolean leveled = false;

        while (level < maxLevel && xp >= getXpRequired(level)) {

            xp -= getXpRequired(level);
            level++;
            leveled = true;

            // ✅ recompensa económica
            if (rewardPerLevel > 0) {
                economy.deposit(uuid, rewardPerLevel);
                totalCoins += rewardPerLevel;
            }
        }

        store.setLevel(uuid, level);
        store.setXp(uuid, xp);

        if (leveled) {
            String message = "§aSubiste para o §eNível " + level + "§a!";

            if (totalCoins > 0) {
                message += " Recebeste §e" + totalCoins + " coins§a.";
            }

            player.sendMessage(message);

            // 🔥 opcional: título estilo servidor grande
            player.sendTitle("§6LEVEL UP!", "§eNível " + level, 10, 40, 10);

            return new LevelUpResult(true, level, totalCoins, message);
        }

        return new LevelUpResult(false, level, 0, null);
    }

    /*
     * ✅ UTIL PARA PLACEHOLDERS FUTUROS
     */
    public double getXpProgress(UUID uuid) {
        int level = getLevel(uuid);
        int xp = getXp(uuid);
        int required = getXpRequired(level);

        if (required == 0) return 0;

        return (double) xp / required;
    }

    public int getXpRemaining(UUID uuid) {
        int level = getLevel(uuid);
        return getXpRequired(level) - getXp(uuid);
    }

    public interface EconomyServiceAdapter {
        void deposit(UUID uuid, int amount);
    }
}
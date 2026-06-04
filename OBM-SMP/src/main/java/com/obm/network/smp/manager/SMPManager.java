package com.obm.network.smp.manager;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.integration.AltDetectionBridge;
import com.obm.network.core.integration.EmeraldRewardBridge;
import com.obm.network.core.integration.EconomyBridge;
import com.obm.network.core.integration.SMPBridge;
import com.obm.network.core.location.SafeSpawnService;
import com.obm.network.core.storage.DataStore;
import com.obm.network.core.world.WorldModeService;
import com.obm.network.smp.progression.LevelService;
import com.obm.network.smp.progression.PlayerProgressionStore;
import com.obm.network.smp.progression.ProgressionBonusService;
import com.obm.network.smp.progression.RankCatalog;
import com.obm.network.smp.service.EconomyService;
import com.obm.network.smp.retention.RetentionFeedback;
import com.obm.network.smp.service.KillFarmGuard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SMPManager {

    private final EconomyService economyService;
    private final WorldModeService worldModeService;
    private final DataStore dataStore;

    private final ProgressionBonusService bonusService;
    private final LevelService levelService;
    private final PlayerProgressionStore progressionStore;
    private final RankCatalog rankCatalog;
    private final KillFarmGuard killFarmGuard;

    private final int killReward;
    private final int deathPenalty;
    private final boolean deathPenaltyEnabled;

    public SMPManager(EconomyService economyService,
                      WorldModeService worldModeService,
                      ProgressionBonusService bonusService,
                      LevelService levelService,
                      PlayerProgressionStore progressionStore,
                      RankCatalog rankCatalog,
                      KillFarmGuard killFarmGuard,
                      int killReward,
                      int deathPenalty,
                      boolean deathPenaltyEnabled) {

        this.economyService = economyService;
        this.worldModeService = worldModeService;
        this.dataStore = OBMCorePlugin.get().getDataStore();
        this.bonusService = bonusService;
        this.levelService = levelService;
        this.progressionStore = progressionStore;
        this.rankCatalog = rankCatalog;
        this.killFarmGuard = killFarmGuard;

        this.killReward = Math.max(0, killReward);
        this.deathPenalty = Math.max(0, deathPenalty);
        this.deathPenaltyEnabled = deathPenaltyEnabled;
    }

    /*
     * ✅ ENTER
     */
    public boolean handleEnter(Player player) {

        String worldName = worldModeService.getPrimarySMPWorld();
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage("§cMundo SMP não encontrado.");
            return false;
        }

        Location spawn = SafeSpawnService.getSafeSpawn(world);
        if (spawn == null) {
            player.sendMessage("§cErro ao encontrar spawn seguro.");
            return false;
        }

        player.teleport(spawn);
        handleJoin(player);

        player.sendMessage("§aEntraste no SMP!");
        return true;
    }

    /*
     * ✅ JOIN
     */
    public void handleJoin(Player player) {

        if (!isInSMP(player)) return;

        progressionStore.ensureInitialized(
                player.getUniqueId(),
                rankCatalog.getDefaultRankId()
        );

        int coins = EconomyBridge.getBalance(player.getUniqueId());
        int level = SMPBridge.getProgression(player.getUniqueId()).level();
        RetentionFeedback.smpWelcome(player, coins, level);
    }

    /*
     * ✅ KILL SYSTEM
     */
    public void handleKill(Player killer, Player victim) {

        if (!isInSMP(killer) || !isInSMP(victim)) return;
        if (killer.getUniqueId().equals(victim.getUniqueId())) return;

        UUID killerId = killer.getUniqueId();
        UUID victimId = victim.getUniqueId();

        boolean altBlocked = AltDetectionBridge.shouldBlockKillReward(killer, victim);
        boolean farmBlocked = !killFarmGuard.canRewardKill(killerId, victimId);
        boolean blocked = altBlocked || farmBlocked;

        int reward = bonusService.applyKillReward(killerId, killReward);

        if (!blocked && reward > 0) {
            economyService.deposit(killerId, reward);
            RetentionFeedback.coinsGained(killer, reward);
            EmeraldRewardBridge.smpKill(killer, victim);
            com.obm.network.core.integration.BattlePassBridge.smpKill(killer);
            killer.sendMessage("§7Mataste §f" + victim.getName());
        } else if (altBlocked) {
            killer.sendMessage("§7Sem recompensa (mesmo IP — anti-alt).");
        } else {
            killer.sendMessage("§7Sem recompensa (anti farm).");
        }

        /*
         * ✅ STATS (SMP)
         */
        dataStore.increment(killerId, "kills_smp");

        int streak = dataStore.getInt(killerId, "killstreak_smp") + 1;
        dataStore.set(killerId, "killstreak_smp", streak);

        int best = dataStore.getInt(killerId, "best_killstreak_smp");
        if (streak > best) {
            dataStore.set(killerId, "best_killstreak_smp", streak);
        }

        dataStore.set(victimId, "killstreak_smp", 0);

        dataStore.save(killerId);

        levelService.addKillXp(killer);
    }

    /*
     * ✅ DEATH SYSTEM
     */
    public void handleDeath(Player victim, Player killer) {

        if (!isInSMP(victim)) return;

        UUID victimId = victim.getUniqueId();

        // ✅ stats SMP
        dataStore.increment(victimId, "deaths_smp");

        dataStore.set(victimId, "killstreak_smp", 0);

        dataStore.save(victimId);

        if (deathPenaltyEnabled && deathPenalty > 0) {

            int penalty = Math.min(deathPenalty,
                    economyService.getBalance(victimId));

            if (penalty > 0 &&
                economyService.safeWithdraw(victimId, penalty)) {

                victim.sendMessage("§c-§e" + economyService.format(penalty)
                        + " §c| Morreste no SMP");
            }
        }

        if (killer != null && isInSMP(killer)) {
            handleKill(killer, victim);
        }
    }

    /*
     * ✅ PLAYTIME REWARD
     */
    public void handlePlaytimeReward(Player player, int reward) {

        if (!isInSMP(player) || reward <= 0) return;

        int payout = bonusService.applyPlaytimeReward(player.getUniqueId(), reward);
        economyService.deposit(player.getUniqueId(), payout);
        RetentionFeedback.coinsGained(player, payout);
        EmeraldRewardBridge.smpPlaytime(player);

        levelService.addPlaytimeXp(player);
    }

    private boolean isInSMP(Player player) {
        return worldModeService.isSMP(player.getWorld().getName());
    }
}
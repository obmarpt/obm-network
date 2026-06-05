package com.obm.network.tierspace.hub;

import com.obm.network.tierspace.TierSpacePlugin;
import com.obm.network.tierspace.party.PartyService;
import com.obm.network.tierspace.queue.QueueFeedbackService;
import com.obm.network.tierspace.queue.QueueService;
import com.obm.network.tierspace.ui.RankedHubItems;
import com.obm.network.tierspace.ui.TierMenuColors;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inventário fixo do hub rankedSpawn (TierSpace).
 */
public final class TierSpaceHubService {

    private final TierSpacePlugin plugin;
    private final QueueService queueService;
    private final QueueFeedbackService queueFeedbackService;
    private final ConcurrentHashMap<UUID, Long> applyGuard = new ConcurrentHashMap<>();

    public TierSpaceHubService(TierSpacePlugin plugin,
                               QueueService queueService,
                               QueueFeedbackService queueFeedbackService) {
        this.plugin = plugin;
        this.queueService = queueService;
        this.queueFeedbackService = queueFeedbackService;
    }

    public boolean isInTierSpaceHub(Player player) {
        return TierSpaceHub.isInTierSpaceHub(player);
    }

    /**
     * Entrada no modo TierSpace — reset completo + itens de hub.
     */
    public void handleTierSpaceJoin(Player player) {
        handleTierSpaceJoin(player, false);
    }

    public void handleTierSpaceJoin(Player player, boolean force) {
        if (player == null || !player.isOnline() || !isInTierSpaceHub(player)) {
            return;
        }
        if (plugin.getMatchService() != null && plugin.getMatchService().isInMatch(player.getUniqueId())) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();
        if (!force) {
            Long last = applyGuard.get(uuid);
            if (last != null && now - last < 250L) {
                return;
            }
            if (player.hasMetadata(TierSpaceHubKeys.META_EQUIPPED) && hasValidHubLoadout(player)) {
                return;
            }
        }

        applyGuard.put(uuid, now);
        clearHubState(player);
        resetPlayerForHub(player);
        applyHubItems(player);
        player.setMetadata(TierSpaceHubKeys.META_EQUIPPED, new FixedMetadataValue(plugin, true));
    }

    public void handleTierSpaceLeave(Player player) {
        if (player == null) {
            return;
        }
        clearHubState(player);
        leaveQueueSilently(player);
    }

    public void leaveQueueAndRefresh(Player player) {
        if (player == null) {
            return;
        }
        leaveQueueSilently(player);
        if (isInTierSpaceHub(player)) {
            handleTierSpaceJoin(player, true);
        }
    }

    private void clearHubState(Player player) {
        applyGuard.remove(player.getUniqueId());
        if (player.hasMetadata(TierSpaceHubKeys.META_EQUIPPED)) {
            player.removeMetadata(TierSpaceHubKeys.META_EQUIPPED, plugin);
        }
        RankedHubItems.stripHubItems(player);
    }

    private void resetPlayerForHub(Player player) {
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private void applyHubItems(Player player) {
        var cfg = plugin.getConfig();
        int queueSlot = clampHotbar(cfg.getInt("ranked-spawn.queue-item.slot", 0));
        int partySlot = clampHotbar(cfg.getInt("ranked-spawn.party-item.slot", 1));
        int leaveSlot = clampHotbar(cfg.getInt("ranked-spawn.leave-item.slot", 8));

        String queueName = cfg.getString("ranked-spawn.queue-item.name",
                TierMenuColors.bold("§9⚔ ") + TierMenuColors.primary("QUEUE DUELS"));
        player.getInventory().setItem(queueSlot, RankedHubItems.queueSword(plugin, queueName, List.of(
                TierMenuColors.separator(),
                TierMenuColors.neutral("Modos PvP competitivos"),
                TierMenuColors.primary("▶ Abrir filas")
        )));

        PartyService parties = plugin.getPartyService();
        boolean inParty = parties != null && parties.inParty(player.getUniqueId());
        String partyName = cfg.getString("ranked-spawn.party-item.name",
                TierMenuColors.bold(TierMenuColors.accent("👥 PARTY")));
        player.getInventory().setItem(partySlot, RankedHubItems.partyItem(plugin, partyName, List.of(
                TierMenuColors.separator(),
                inParty ? TierMenuColors.success("Gerir party") : TierMenuColors.neutral("Criar party"),
                TierMenuColors.primary("▶ Clique")
        )));

        String leaveName = cfg.getString("ranked-spawn.leave-item.name",
                TierMenuColors.bold(TierMenuColors.error("✖ SAIR DA FILA")));
        player.getInventory().setItem(leaveSlot, RankedHubItems.leaveItem(plugin, leaveName, List.of(
                TierMenuColors.separator(),
                TierMenuColors.neutral("Sai da fila de matchmaking"),
                TierMenuColors.primary("▶ Clique ou /leave")
        )));
    }

    private boolean hasValidHubLoadout(Player player) {
        var cfg = plugin.getConfig();
        int q = clampHotbar(cfg.getInt("ranked-spawn.queue-item.slot", 0));
        int p = clampHotbar(cfg.getInt("ranked-spawn.party-item.slot", 1));
        int l = clampHotbar(cfg.getInt("ranked-spawn.leave-item.slot", 8));
        return RankedHubItems.isQueueSword(player.getInventory().getItem(q))
                && RankedHubItems.isPartyItem(player.getInventory().getItem(p))
                && RankedHubItems.isLeaveItem(player.getInventory().getItem(l))
                && RankedHubItems.countHubItems(player) <= 3;
    }

    private void leaveQueueSilently(Player player) {
        if (queueService.leave(player.getUniqueId())) {
            queueFeedbackService.stopTracking(player.getUniqueId());
        }
    }

    private static int clampHotbar(int slot) {
        return Math.max(0, Math.min(8, slot));
    }
}

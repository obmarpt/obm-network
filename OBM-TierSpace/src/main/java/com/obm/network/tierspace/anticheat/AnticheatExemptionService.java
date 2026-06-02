package com.obm.network.tierspace.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AnticheatExemptionService {

    private static final String GRIM_EXEMPT = "grim.exempt";
    private static final String SPARTAN_BYPASS = "spartan.bypass";

    private final JavaPlugin plugin;
    private final AnticheatSettings settings;
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    private final Set<UUID> exemptPlayers = ConcurrentHashMap.newKeySet();
    private int refreshTaskId = -1;

    public AnticheatExemptionService(JavaPlugin plugin, AnticheatSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void start() {
        if (refreshTaskId != -1) {
            return;
        }
        refreshTaskId = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::refreshAllOnline,
                settings.exemptionRefreshTicks(),
                settings.exemptionRefreshTicks()
        ).getTaskId();
    }

    public void stop() {
        if (refreshTaskId != -1) {
            Bukkit.getScheduler().cancelTask(refreshTaskId);
            refreshTaskId = -1;
        }
        for (UUID uuid : attachments.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                revokeExemption(player);
            }
        }
        attachments.clear();
        exemptPlayers.clear();
    }

    public void refreshAllOnline() {
        double tps = ServerPerformanceMonitor.getTps();
        for (Player player : Bukkit.getOnlinePlayers()) {
            handleAnticheatExemption(player, tps);
        }
    }

    /**
     * @return true if the player is currently exempt from anticheat checks
     */
    public boolean handleAnticheatExemption(Player player) {
        return handleAnticheatExemption(player, ServerPerformanceMonitor.getTps());
    }

    public boolean handleAnticheatExemption(Player player, double tps) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        boolean shouldExempt = tps < settings.minTps() || player.getPing() > settings.maxPingMs();
        if (shouldExempt) {
            grantExemption(player);
        } else {
            revokeExemption(player);
        }
        return shouldExempt;
    }

    public boolean isExempt(UUID uuid) {
        return exemptPlayers.contains(uuid);
    }

    public boolean isExempt(Player player) {
        return player != null && exemptPlayers.contains(player.getUniqueId());
    }

    public boolean isIntegrationActive() {
        return settings.isAnticheatIntegrationActive(ServerPerformanceMonitor.getTps());
    }

    private void grantExemption(Player player) {
        UUID uuid = player.getUniqueId();
        exemptPlayers.add(uuid);

        if (attachments.containsKey(uuid)) {
            return;
        }

        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission(GRIM_EXEMPT, true);
        attachment.setPermission(SPARTAN_BYPASS, true);
        attachments.put(uuid, attachment);

        SpartanBridge.applyTimedBypass(player, 60);
    }

    private void revokeExemption(Player player) {
        UUID uuid = player.getUniqueId();
        exemptPlayers.remove(uuid);

        PermissionAttachment attachment = attachments.remove(uuid);
        if (attachment != null) {
            attachment.remove();
        }
    }
}

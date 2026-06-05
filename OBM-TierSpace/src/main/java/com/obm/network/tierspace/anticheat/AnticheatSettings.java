package com.obm.network.tierspace.anticheat;

import org.bukkit.configuration.file.FileConfiguration;

public record AnticheatSettings(
        double minTps,
        int maxPingMs,
        boolean discordEnabled,
        String discordWebhookUrl,
        int grimReachAlertVl,
        int grimHitboxesAlertVl,
        int spartanKillauraAlertVl,
        boolean punishEnabled,
        int tempbanDays,
        int grimReachBanVl,
        int grimHitboxesBanVl,
        int spartanKillauraBanVl,
        int exemptionRefreshTicks
) {
    public static AnticheatSettings from(FileConfiguration config) {
        return new AnticheatSettings(
                config.getDouble("anticheat.min-tps", 19.5),
                config.getInt("anticheat.max-ping-ms", 350),
                config.getBoolean("anticheat.discord.enabled", false),
                config.getString("anticheat.discord.webhook-url", ""),
                config.getInt("anticheat.alerts.grim-reach-vl", 3),
                config.getInt("anticheat.alerts.grim-hitboxes-vl", 3),
                config.getInt("anticheat.alerts.spartan-killaura-vl", 5),
                config.getBoolean("anticheat.punish.enabled", true),
                config.getInt("anticheat.punish.tempban-days", 30),
                config.getInt("anticheat.punish.grim-reach-vl", 10),
                config.getInt("anticheat.punish.grim-hitboxes-vl", 10),
                config.getInt("anticheat.punish.spartan-killaura-vl", 12),
                Math.max(20, config.getInt("anticheat.exemption-refresh-ticks", 40))
        );
    }

    public boolean isAnticheatIntegrationActive(double tps) {
        return tps >= minTps;
    }
}

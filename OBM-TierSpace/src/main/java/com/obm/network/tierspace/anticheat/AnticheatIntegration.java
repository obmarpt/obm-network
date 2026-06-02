package com.obm.network.tierspace.anticheat;

import org.bukkit.plugin.java.JavaPlugin;

public class AnticheatIntegration {

    private final JavaPlugin plugin;
    private final AnticheatSettings settings;
    private final AnticheatExemptionService exemptionService;
    private final MatchProtectionService matchProtectionService;
    private final DiscordWebhookService discordWebhookService;
    private final AnticheatAlertService alertService;
    private final GrimBridge grimBridge;
    private final SpartanEventBridge spartanEventBridge;

    public AnticheatIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
        this.settings = AnticheatSettings.from(plugin.getConfig());
        this.exemptionService = new AnticheatExemptionService(plugin, settings);
        this.matchProtectionService = new MatchProtectionService();
        this.discordWebhookService = new DiscordWebhookService(plugin, settings);
        this.alertService = new AnticheatAlertService(plugin, settings, exemptionService, discordWebhookService);
        this.grimBridge = new GrimBridge(plugin, exemptionService, alertService::handleFlag);
        this.spartanEventBridge = new SpartanEventBridge(plugin, alertService, exemptionService);
    }

    public void start() {
        exemptionService.start();
        grimBridge.register();
        spartanEventBridge.register();
        plugin.getLogger().info("[Anticheat] Integração TierSpace activa (TPS min=" + settings.minTps()
                + ", ping max=" + settings.maxPingMs() + "ms).");
    }

    public void stop() {
        grimBridge.unregister();
        exemptionService.stop();
    }

    public AnticheatExemptionService exemptionService() {
        return exemptionService;
    }

    public MatchProtectionService matchProtectionService() {
        return matchProtectionService;
    }
}

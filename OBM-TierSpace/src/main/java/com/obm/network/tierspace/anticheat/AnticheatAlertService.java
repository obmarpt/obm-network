package com.obm.network.tierspace.anticheat;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AnticheatAlertService {

    private final JavaPlugin plugin;
    private final AnticheatSettings settings;
    private final AnticheatExemptionService exemptionService;
    private final DiscordWebhookService discordWebhookService;
    private final Map<String, Double> lastAlertVl = new ConcurrentHashMap<>();

    public AnticheatAlertService(JavaPlugin plugin,
                                 AnticheatSettings settings,
                                 AnticheatExemptionService exemptionService,
                                 DiscordWebhookService discordWebhookService) {
        this.plugin = plugin;
        this.settings = settings;
        this.exemptionService = exemptionService;
        this.discordWebhookService = discordWebhookService;
    }

    public void handleFlag(AnticheatFlag flag) {
        if (!exemptionService.isIntegrationActive()) {
            return;
        }

        Player player = flag.player();
        if (player == null || exemptionService.isExempt(player)) {
            return;
        }

        String normalizedCheck = normalizeCheck(flag.source(), flag.check());
        if (!shouldTrack(normalizedCheck)) {
            return;
        }

        int alertThreshold = alertThreshold(normalizedCheck);
        int banThreshold = banThreshold(normalizedCheck);

        if (flag.vl() < alertThreshold) {
            return;
        }

        String dedupeKey = player.getUniqueId() + ":" + normalizedCheck;
        Double previous = lastAlertVl.get(dedupeKey);
        if (previous != null && Math.abs(previous - flag.vl()) < 0.5) {
            return;
        }
        lastAlertVl.put(dedupeKey, flag.vl());

        plugin.getLogger().warning(String.format(Locale.US,
                "[TierSpace AC] %s flagged %s (%s) VL=%.1f",
                flag.source(), player.getName(), flag.check(), flag.vl()));

        discordWebhookService.sendAnticheatAlert(flag);

        if (settings.punishEnabled() && flag.vl() >= banThreshold) {
            LiteBansBridge.tempban(player, settings.tempbanDays(), "Cheating (TierSpace)");
        }
    }

    public void handleSpartanFlag(Player player, String check, double vl) {
        handleFlag(new AnticheatFlag("Spartan", player, check, vl, ""));
    }

    private boolean shouldTrack(String normalizedCheck) {
        return normalizedCheck.equals("grim:reach")
                || normalizedCheck.equals("grim:hitboxes")
                || normalizedCheck.equals("spartan:killaura");
    }

    private int alertThreshold(String normalizedCheck) {
        return switch (normalizedCheck) {
            case "grim:reach" -> settings.grimReachAlertVl();
            case "grim:hitboxes" -> settings.grimHitboxesAlertVl();
            case "spartan:killaura" -> settings.spartanKillauraAlertVl();
            default -> Integer.MAX_VALUE;
        };
    }

    private int banThreshold(String normalizedCheck) {
        return switch (normalizedCheck) {
            case "grim:reach" -> settings.grimReachBanVl();
            case "grim:hitboxes" -> settings.grimHitboxesBanVl();
            case "spartan:killaura" -> settings.spartanKillauraBanVl();
            default -> Integer.MAX_VALUE;
        };
    }

    private String normalizeCheck(String source, String check) {
        String lower = check.toLowerCase(Locale.ROOT);
        if ("Grim".equalsIgnoreCase(source)) {
            if (lower.contains("reach")) {
                return "grim:reach";
            }
            if (lower.contains("hitbox")) {
                return "grim:hitboxes";
            }
        }
        if ("Spartan".equalsIgnoreCase(source)) {
            if (lower.contains("killaura") || lower.contains("kill aura")) {
                return "spartan:killaura";
            }
        }
        return source.toLowerCase(Locale.ROOT) + ":" + lower;
    }
}

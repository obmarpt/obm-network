package com.obm.network.tierspace.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookService {

    private final JavaPlugin plugin;
    private final AnticheatSettings settings;

    public DiscordWebhookService(JavaPlugin plugin, AnticheatSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void sendAnticheatAlert(AnticheatFlag flag) {
        if (!settings.discordEnabled()) {
            return;
        }
        String url = settings.discordWebhookUrl();
        if (url == null || url.isBlank()) {
            return;
        }

        String playerName = flag.player() != null ? flag.player().getName() : "Unknown";
        String payload = """
                {
                  "embeds": [{
                    "title": "TierSpace Anticheat Alert",
                    "color": 16711680,
                    "fields": [
                      {"name": "Player", "value": "%s", "inline": true},
                      {"name": "Check", "value": "%s", "inline": true},
                      {"name": "VL", "value": "%.1f", "inline": true},
                      {"name": "Source", "value": "%s", "inline": true},
                      {"name": "Mode", "value": "TierSpace", "inline": true}
                    ],
                    "description": "%s"
                  }]
                }
                """.formatted(
                escapeJson(playerName),
                escapeJson(flag.check()),
                flag.vl(),
                escapeJson(flag.source()),
                escapeJson(flag.verbose() == null ? "" : flag.verbose())
        );

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> post(url, payload));
    }

    private void post(String url, String payload) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (OutputStream stream = connection.getOutputStream()) {
                stream.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            connection.getResponseCode();
            connection.disconnect();
        } catch (Exception e) {
            plugin.getLogger().warning("[Anticheat] Discord webhook falhou: " + e.getMessage());
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}

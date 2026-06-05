package com.obm.network.smp.spawn;

import com.obm.network.smp.retention.RetentionFeedback;
import com.obm.network.smp.service.SpawnProtectionService;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
public final class SpawnBoundaryVisualizer {

    private final SpawnProtectionService protection;

    public SpawnBoundaryVisualizer(SpawnProtectionService protection) {
        this.protection = protection;
    }

    public void tick() {
        SpawnBoundarySettings settings = protection.settings();
        if (!settings.enabled() || !settings.showBoundary()) {
            return;
        }

        Location center = protection.getSpawnCenter();
        if (center == null || center.getWorld() == null) {
            return;
        }

        double radius = protection.getRadius();
        double renderDistSq = settings.renderDistance() * settings.renderDistance();
        int points = settings.particlePoints();

        for (Player player : center.getWorld().getPlayers()) {
            if (!player.isOnline() || player.getLocation().distanceSquared(center) > renderDistSq) {
                continue;
            }
            if (settings.showDistanceWarning()) {
                sendProximityFeedback(player);
            }
            if (!settings.showParticles()) {
                continue;
            }
            spawnParticlesNearPlayer(player, center, radius, points, settings);
        }
    }

    private void sendProximityFeedback(Player player) {
        if (protection.isNearBoundary(player.getLocation())) {
            RetentionFeedback.sendActionBar(player, "§cEstás a sair da zona segura");
        } else if (protection.isInSpawnProtection(player.getLocation())) {
            RetentionFeedback.sendActionBar(player, "§aZona segura");
        }
    }

    private void spawnParticlesNearPlayer(Player player, Location center, double radius,
                                          int points, SpawnBoundarySettings settings) {
        double playerDistSq = settings.renderDistance() * 0.6;
        playerDistSq *= playerDistSq;
        Particle particle = settings.particleType();
        boolean dust = particle == Particle.DUST;
        Particle.DustOptions dustOptions = dust
                ? new Particle.DustOptions(Color.fromRGB(85, 255, 85), 1.2f)
                : null;

        for (int i = 0; i < points; i++) {
            double angle = (2.0 * Math.PI * i) / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            double y = player.getLocation().getY() + 0.2;

            double dx = x - player.getX();
            double dz = z - player.getZ();
            if ((dx * dx + dz * dz) > playerDistSq) {
                continue;
            }

            Location particleLoc = new Location(center.getWorld(), x, y, z);
            if (dust && dustOptions != null) {
                player.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions);
            } else {
                player.spawnParticle(particle, particleLoc, 1, 0, 0.05, 0, 0.01);
            }
        }
    }
}

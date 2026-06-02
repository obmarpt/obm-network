package com.obm.network.lobby.hardcore;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.hardcore.HardcoreUnlockService;
import com.obm.network.core.hardcore.HardcoreUnlockStatus;
import com.obm.network.core.storage.DataStore;
import com.obm.network.lobby.OBMLobbyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Efeito visual/audio na primeira vez que o jogador desbloqueia o Hardcore.
 */
public final class HardcoreUnlockCelebration {

    private static final String CELEBRATED_KEY = "hardcore_unlock_celebrated";

    /** Title: fadeIn, stay, fadeOut (ticks) — snappy mas legível */
    private static final int TITLE_FADE_IN = 5;
    private static final int TITLE_STAY = 45;
    private static final int TITLE_FADE_OUT = 10;

    private static final long CHAT_DELAY_TICKS = 10L;
    private static final long GLOW_DURATION_TICKS = 50L;
    private static final long PARTICLE_PULSE_DELAY_TICKS = 6L;

    private HardcoreUnlockCelebration() {
    }

    public static void tryCelebrate(Player player) {
        HardcoreUnlockStatus status = HardcoreUnlockService.evaluate(player.getUniqueId());
        if (!status.gateEnabled() || !status.unlocked()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        DataStore ds = OBMCorePlugin.get().getDataStore();
        if (ds.getBoolean(uuid, CELEBRATED_KEY)) {
            return;
        }

        ds.setBoolean(uuid, CELEBRATED_KEY, true);
        ds.save(uuid);

        playUnlockEffect(player);

        OBMLobbyPlugin plugin = OBMLobbyPlugin.get();
        if (plugin != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> sendUnlockChat(player), CHAT_DELAY_TICKS);
        } else {
            sendUnlockChat(player);
        }
    }

    public static void playUnlockEffect(Player player) {
        player.sendTitle(
                "§c§l🎉 HARDCORE DESBLOQUEADO",
                "§7Já podes entrar no Hardcore!",
                TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT
        );

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        spawnUnlockParticles(player);

        player.setGlowing(true);
        OBMLobbyPlugin plugin = OBMLobbyPlugin.get();
        if (plugin != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setGlowing(false);
                }
            }, GLOW_DURATION_TICKS);
        }
    }

    /** Partículas leves — TOTEM (burst) + END_ROD (pulse), sem spam. */
    private static void spawnUnlockParticles(Player player) {
        var world = player.getWorld();
        var center = player.getLocation().add(0, 1.1, 0);

        world.spawnParticle(Particle.TOTEM, center, 24, 0.35, 0.55, 0.35, 0.08);
        world.spawnParticle(Particle.END_ROD, center, 12, 0.45, 0.35, 0.45, 0.04);

        OBMLobbyPlugin plugin = OBMLobbyPlugin.get();
        if (plugin == null) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            var pulse = player.getLocation().add(0, 1.0, 0);
            player.getWorld().spawnParticle(Particle.END_ROD, pulse, 8, 0.3, 0.25, 0.3, 0.02);
        }, PARTICLE_PULSE_DELAY_TICKS);
    }

    private static void sendUnlockChat(Player player) {
        if (!player.isOnline()) {
            return;
        }
        player.sendMessage("");
        player.sendMessage("§a§l✅ Hardcore desbloqueado!");
        player.sendMessage("§7§e§l👉 Clica para jogar §7no menu.");
        player.sendMessage("");
    }
}

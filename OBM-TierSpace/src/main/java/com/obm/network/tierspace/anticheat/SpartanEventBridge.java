package com.obm.network.tierspace.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Locale;

public class SpartanEventBridge implements Listener {

    private final JavaPlugin plugin;
    private final AnticheatAlertService alertService;
    private final AnticheatExemptionService exemptionService;
    private boolean registered;

    public SpartanEventBridge(JavaPlugin plugin,
                              AnticheatAlertService alertService,
                              AnticheatExemptionService exemptionService) {
        this.plugin = plugin;
        this.alertService = alertService;
        this.exemptionService = exemptionService;
    }

    public void register() {
        if (registered || Bukkit.getPluginManager().getPlugin("Spartan") == null) {
            return;
        }

        String[] candidates = {
                "dev.spartan.events.SpartanViolationEvent",
                "me.vagdedes.spartan.system.check.events.SpartanViolationEvent",
                "me.vagdedes.spartan.api.SpartanViolationEvent"
        };

        for (String className : candidates) {
            if (tryRegister(className)) {
                registered = true;
                plugin.getLogger().info("[Anticheat] Spartan event integrado: " + className);
                return;
            }
        }

        plugin.getLogger().info("[Anticheat] Spartan event não encontrado — usa permissão spartan.bypass + alertas Grim.");
    }

    private boolean tryRegister(String className) {
        try {
            Class<? extends Event> eventClass = Class.forName(className).asSubclass(Event.class);
            EventExecutor executor = (listener, event) -> handleViolationEvent(event);
            Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    this,
                    EventPriority.MONITOR,
                    executor,
                    plugin,
                    true
            );
            return true;
        } catch (ClassNotFoundException | ClassCastException ignored) {
            return false;
        }
    }

    private void handleViolationEvent(Event event) {
        if (!exemptionService.isIntegrationActive()) {
            return;
        }

        try {
            Player player = extractPlayer(event);
            if (player == null) {
                return;
            }

            if (exemptionService.handleAnticheatExemption(player)) {
                return;
            }

            String check = extractString(event, "getCheck", "getCheckName", "getHackType", "getDetection");
            double vl = extractDouble(event, "getViolations", "getViolationLevel", "getVl", "getLevel");

            if (check == null) {
                check = event.getClass().getSimpleName();
            }

            String lower = check.toLowerCase(Locale.ROOT);
            if (lower.contains("killaura") || lower.contains("kill aura") || lower.contains("aura")) {
                alertService.handleSpartanFlag(player, check, vl);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private Player extractPlayer(Event event) throws ReflectiveOperationException {
        for (String methodName : new String[]{"getPlayer", "getBukkitPlayer", "getEntity"}) {
            try {
                Method method = event.getClass().getMethod(methodName);
                Object value = method.invoke(event);
                if (value instanceof Player player) {
                    return player;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private String extractString(Event event, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = event.getClass().getMethod(methodName);
                Object value = method.invoke(event);
                if (value != null) {
                    return String.valueOf(value);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private double extractDouble(Event event, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = event.getClass().getMethod(methodName);
                Object value = method.invoke(event);
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return 0;
    }
}

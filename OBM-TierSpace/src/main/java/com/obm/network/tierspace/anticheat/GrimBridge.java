package com.obm.network.tierspace.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;

public class GrimBridge {

    private final JavaPlugin plugin;
    private final AnticheatExemptionService exemptionService;
    private final Consumer<AnticheatFlag> flagHandler;
    private Object grimPluginHandle;
    private boolean hooked;

    public GrimBridge(JavaPlugin plugin,
                      AnticheatExemptionService exemptionService,
                      Consumer<AnticheatFlag> flagHandler) {
        this.plugin = plugin;
        this.exemptionService = exemptionService;
        this.flagHandler = flagHandler;
    }

    public void register() {
        if (Bukkit.getPluginManager().getPlugin("GrimAC") == null) {
            plugin.getLogger().info("[Anticheat] GrimAC não encontrado — integração Grim desactivada.");
            return;
        }

        try {
            Class<?> providerClass = Class.forName("ac.grim.grimac.api.GrimAPIProvider");
            Method getApi = providerClass.getMethod("get");
            Object api = getApi.invoke(null);

            Method getGrimPlugin = api.getClass().getMethod("getGrimPlugin", org.bukkit.plugin.Plugin.class);
            grimPluginHandle = getGrimPlugin.invoke(api, plugin);

            Method getEventBus = api.getClass().getMethod("getEventBus");
            Object eventBus = getEventBus.invoke(api);

            Class<?> flagEventClass = Class.forName("ac.grim.grimac.api.event.events.FlagEvent");
            Method getChannel = eventBus.getClass().getMethod("get", Class.class);
            Object channel = getChannel.invoke(eventBus, flagEventClass);

            Method onFlag = channel.getClass().getMethod("onFlag",
                    Class.forName("ac.grim.grimac.api.plugin.GrimPlugin"),
                    java.util.function.Function.class);

            Object handler = (java.util.function.Function<Object[], Object>) args -> {
                Object user = args[0];
                Object check = args[1];
                String verbose = String.valueOf(args[2]);
                boolean cancelled = (boolean) args[3];

                UUID uuid = extractUuid(user);
                if (uuid == null) {
                    return cancelled;
                }

                Player player = Bukkit.getPlayer(uuid);
                if (player != null && exemptionService.handleAnticheatExemption(player)) {
                    return true;
                }

                String checkName = extractCheckName(check);
                double vl = extractViolations(check);
                flagHandler.accept(new AnticheatFlag("Grim", player, checkName, vl, verbose));
                return cancelled;
            };

            onFlag.invoke(channel, grimPluginHandle, handler);
            hooked = true;
            plugin.getLogger().info("[Anticheat] Grim FlagEvent integrado.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("[Anticheat] GrimAPI não disponível — usa apenas permissão grim.exempt.");
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("[Anticheat] Falha ao integrar GrimAPI: " + e.getMessage());
        }
    }

    public void unregister() {
        if (!hooked || grimPluginHandle == null) {
            return;
        }
        try {
            Class<?> providerClass = Class.forName("ac.grim.grimac.api.GrimAPIProvider");
            Object api = providerClass.getMethod("get").invoke(null);
            Object eventBus = api.getClass().getMethod("getEventBus").invoke(api);
            eventBus.getClass().getMethod("unregisterAllListeners", grimPluginHandle.getClass().getInterfaces()[0])
                    .invoke(eventBus, grimPluginHandle);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private UUID extractUuid(Object user) {
        try {
            Method getUuid = user.getClass().getMethod("getUniqueId");
            return (UUID) getUuid.invoke(user);
        } catch (ReflectiveOperationException e) {
            try {
                Method getName = user.getClass().getMethod("getName");
                String name = (String) getName.invoke(user);
                Player player = Bukkit.getPlayerExact(name);
                return player != null ? player.getUniqueId() : null;
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
    }

    private String extractCheckName(Object check) {
        try {
            return String.valueOf(check.getClass().getMethod("getCheckName").invoke(check));
        } catch (ReflectiveOperationException e) {
            return check.getClass().getSimpleName();
        }
    }

    private double extractViolations(Object check) {
        try {
            return ((Number) check.getClass().getMethod("getViolations").invoke(check)).doubleValue();
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }
}

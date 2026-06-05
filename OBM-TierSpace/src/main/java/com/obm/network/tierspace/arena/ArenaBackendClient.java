package com.obm.network.tierspace.arena;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.backend.BackendConfig;
import com.obm.network.core.backend.HttpUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class ArenaBackendClient {

    private final JavaPlugin plugin;
    private final Logger logger;

    public ArenaBackendClient(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean isEnabled() {
        OBMCorePlugin core = OBMCorePlugin.get();
        return core != null && core.getBackendService() != null && core.getBackendService().isEnabled();
    }

    private BackendConfig config() {
        return new BackendConfig(OBMCorePlugin.get());
    }

    public CompletableFuture<List<ArenaDefinition>> fetchArenasAsync() {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(List.of());
        }
        BackendConfig cfg = config();
        String url = cfg.url("/admin/arenas");
        return CompletableFuture.supplyAsync(() -> {
            HttpUtils.Response response = HttpUtils.get(
                    url,
                    cfg.getConnectTimeoutMs(),
                    cfg.getReadTimeoutMs(),
                    cfg.getPluginKey(),
                    logger,
                    cfg.isDebug());
            if (!response.isSuccess() || response.body() == null) {
                return List.<ArenaDefinition>of();
            }
            return ArenaJsonParser.parseList(response.body());
        });
    }

    public void fetchArenas(ArenaService arenaService) {
        fetchArenasAsync().thenAccept(list -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!list.isEmpty()) {
                arenaService.mergeBackend(list);
            }
        }));
    }

    public CompletableFuture<Boolean> saveArenaAsync(ArenaDefinition arena) {
        if (!isEnabled() || arena == null) {
            return CompletableFuture.completedFuture(false);
        }
        BackendConfig cfg = config();
        String json = toJson(arena);
        return CompletableFuture.supplyAsync(() -> {
            String url = cfg.url("/admin/arenas");
            HttpUtils.Response create = HttpUtils.post(
                    url, json, cfg.getConnectTimeoutMs(), cfg.getReadTimeoutMs(),
                    cfg.getPluginKey(), logger, cfg.isDebug());
            if (create.statusCode() == 409) {
                HttpUtils.Response update = HttpUtils.put(
                        cfg.url("/admin/arenas/" + arena.id()),
                        json,
                        cfg.getConnectTimeoutMs(),
                        cfg.getReadTimeoutMs(),
                        cfg.getPluginKey(),
                        logger,
                        cfg.isDebug());
                return update.isSuccess();
            }
            return create.isSuccess();
        });
    }

    public CompletableFuture<Boolean> deleteArenaAsync(String id) {
        if (!isEnabled() || id == null) {
            return CompletableFuture.completedFuture(false);
        }
        BackendConfig cfg = config();
        return CompletableFuture.supplyAsync(() -> {
            HttpUtils.Response response = HttpUtils.delete(
                    cfg.url("/admin/arenas/" + id),
                    cfg.getConnectTimeoutMs(),
                    cfg.getReadTimeoutMs(),
                    cfg.getPluginKey(),
                    logger,
                    cfg.isDebug());
            return response.isSuccess();
        });
    }

    static String toJson(ArenaDefinition arena) {
        Location p1 = arena.spawn1();
        Location p2 = arena.spawn2();
        String mode = arena.modes() != null && !arena.modes().isEmpty()
                ? arena.modes().get(0)
                : "sword";
        String world = p1.getWorld() != null ? p1.getWorld().getName() : "TierSpace";
        return "{"
                + HttpUtils.jsonField("id", arena.id()) + ","
                + HttpUtils.jsonField("name", arena.name()) + ","
                + HttpUtils.jsonField("mode", mode) + ","
                + HttpUtils.jsonField("world", world) + ","
                + "\"pos1\":{\"x\":" + p1.getX() + ",\"y\":" + p1.getY() + ",\"z\":" + p1.getZ()
                + ",\"yaw\":" + p1.getYaw() + ",\"pitch\":" + p1.getPitch() + "},"
                + "\"pos2\":{\"x\":" + p2.getX() + ",\"y\":" + p2.getY() + ",\"z\":" + p2.getZ()
                + ",\"yaw\":" + p2.getYaw() + ",\"pitch\":" + p2.getPitch() + "},"
                + "\"enabled\":" + arena.enabled() + ","
                + "\"auto_reset\":" + arena.autoReset()
                + "}";
    }
}

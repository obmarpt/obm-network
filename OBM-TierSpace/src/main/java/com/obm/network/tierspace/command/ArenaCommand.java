package com.obm.network.tierspace.command;

import com.obm.network.tierspace.arena.ArenaBackendClient;
import com.obm.network.tierspace.arena.ArenaDefinition;
import com.obm.network.tierspace.arena.ArenaService;
import com.obm.network.tierspace.arena.ArenaSetupManager;
import com.obm.network.tierspace.mode.GameModeId;
import com.obm.network.tierspace.mode.ModeRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ArenaCommand implements CommandExecutor, TabCompleter {

    private final ArenaService arenaService;
    private final ArenaBackendClient backendClient;
    private final ArenaSetupManager setupManager;
    private final ModeRegistry modeRegistry;

    public ArenaCommand(ArenaService arenaService,
                        ArenaBackendClient backendClient,
                        ArenaSetupManager setupManager,
                        ModeRegistry modeRegistry) {
        this.arenaService = arenaService;
        this.backendClient = backendClient;
        this.setupManager = setupManager;
        this.modeRegistry = modeRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores.");
            return true;
        }
        if (!player.hasPermission("obm.tierspace.arena")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "create" -> handleCreate(player, args);
            case "setpos1" -> handleSetPos1(player);
            case "setpos2" -> handleSetPos2(player);
            case "setmode" -> handleSetMode(player, args);
            case "save" -> handleSave(player);
            case "cancel" -> {
                setupManager.clear(player);
                player.sendMessage("§7Setup de arena cancelado.");
                yield true;
            }
            case "list" -> handleList(player);
            case "reload" -> handleReload(player);
            case "delete" -> handleDelete(player, args);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    private boolean handleCreate(Player player, String[] args) {
        if (!player.hasPermission("obm.tierspace.arena.create")) {
            player.sendMessage("§cSem permissão para criar arenas.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("§c/arena create <nome> [modo]");
            return true;
        }
        String name = args[1];
        String mode = args.length >= 3 ? args[2] : "sword";
        setupManager.startCreate(player, name, mode);
        player.sendMessage("§aArena §f" + name + "§a — modo §f" + mode);
        player.sendMessage("§7Usa §f/arena setpos1§7, §f/arena setpos2§7 e §f/arena save§7.");
        return true;
    }

    private boolean handleSetPos1(Player player) {
        if (setupManager.setPos1(player)) {
            player.sendMessage("§aPosição 1 definida.");
            return true;
        }
        player.sendMessage("§cInicia com §f/arena create <nome>§c.");
        return true;
    }

    private boolean handleSetPos2(Player player) {
        if (setupManager.setPos2(player)) {
            player.sendMessage("§aPosição 2 definida.");
            return true;
        }
        player.sendMessage("§cInicia com §f/arena create <nome>§c.");
        return true;
    }

    private boolean handleSetMode(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c/arena setmode <modo>");
            return true;
        }
        if (setupManager.setMode(player, args[1])) {
            player.sendMessage("§aModo definido: §f" + args[1].toLowerCase(Locale.ROOT));
            return true;
        }
        player.sendMessage("§cModo inválido ou sem setup activo.");
        return true;
    }

    private boolean handleSave(Player player) {
        if (!player.hasPermission("obm.tierspace.arena.create")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }
        ArenaSetupManager.PendingArena pending = setupManager.get(player);
        ArenaDefinition arena = setupManager.build(pending);
        if (arena == null) {
            player.sendMessage("§cCompleta pos1, pos2 e modo antes de guardar.");
            return true;
        }
        player.sendMessage("§7A guardar arena no backend...");
        backendClient.saveArenaAsync(arena).thenAccept(ok ->
                org.bukkit.Bukkit.getScheduler().runTask(
                        org.bukkit.Bukkit.getPluginManager().getPlugin("OBM-TierSpace"),
                        () -> {
                            if (!player.isOnline()) {
                                return;
                            }
                            if (ok) {
                                arenaService.mergeBackend(List.of(arena));
                                setupManager.clear(player);
                                player.sendMessage("§aArena §f" + arena.name() + "§a guardada (§f" + arena.id() + "§a).");
                            } else {
                                player.sendMessage("§cFalha ao guardar — verifica backend.plugin-key.");
                            }
                        }));
        return true;
    }

    private boolean handleList(Player player) {
        List<ArenaDefinition> arenas = arenaService.listArenas();
        if (arenas.isEmpty()) {
            player.sendMessage("§7Nenhuma arena carregada.");
            return true;
        }
        player.sendMessage("§d§lArenas §8(§7" + arenas.size() + "§8)");
        for (ArenaDefinition arena : arenas) {
            String status = arenaService.isOccupied(arena.id()) ? "§cocupada" : "§alivre";
            String on = arena.enabled() ? "§aON" : "§cOFF";
            player.sendMessage("§8• §f" + arena.name() + " §7[" + arena.id() + "] "
                    + on + " §7" + status + " §8— §f" + arena.modes());
        }
        return true;
    }

    private boolean handleReload(Player player) {
        if (!player.hasPermission("obm.tierspace.arena.admin")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }
        backendClient.fetchArenas(arenaService);
        player.sendMessage("§7A sincronizar arenas do backend...");
        return true;
    }

    private boolean handleDelete(Player player, String[] args) {
        if (!player.hasPermission("obm.tierspace.arena.admin")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("§c/arena delete <id>");
            return true;
        }
        String id = args[1];
        backendClient.deleteArenaAsync(id).thenAccept(ok ->
                org.bukkit.Bukkit.getScheduler().runTask(
                        org.bukkit.Bukkit.getPluginManager().getPlugin("OBM-TierSpace"),
                        () -> {
                            if (!player.isOnline()) {
                                return;
                            }
                            if (ok) {
                                arenaService.replaceAll(
                                        arenaService.listArenas().stream()
                                                .filter(a -> !a.id().equals(id))
                                                .toList());
                                player.sendMessage("§aArena removida: §f" + id);
                            } else {
                                player.sendMessage("§cFalha ao remover arena.");
                            }
                        }));
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§d/arena create <nome> [modo]");
        player.sendMessage("§d/arena setpos1 §8| §d/arena setpos2 §8| §d/arena save");
        player.sendMessage("§d/arena list §8| §d/arena reload §8| §d/arena delete <id>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("create", "setpos1", "setpos2", "setmode", "save", "list", "reload", "delete", "cancel"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setmode")) {
            return filter(args[1], modeRegistry.enabledModes().stream().map(GameModeId::id).toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return filter(args[2], modeRegistry.enabledModes().stream().map(GameModeId::id).toList());
        }
        return List.of();
    }

    private static List<String> filter(String prefix, List<String> options) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}

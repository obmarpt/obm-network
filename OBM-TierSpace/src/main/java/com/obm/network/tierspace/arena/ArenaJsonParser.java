package com.obm.network.tierspace.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ArenaJsonParser {

    private static final Pattern ARENA_BLOCK = Pattern.compile(
            "\\{[^{}]*\"id\"\\s*:\\s*\"([^\"]+)\"[^{}]*\\}",
            Pattern.DOTALL);

    private ArenaJsonParser() {
    }

    static List<ArenaDefinition> parseList(String json) {
        List<ArenaDefinition> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        String block = json;
        Matcher array = Pattern.compile("\"arenas\"\\s*:\\s*\\[([\\s\\S]*?)\\]").matcher(json);
        if (array.find()) {
            block = array.group(1);
        }
        Matcher matcher = Pattern.compile(
                "\\{[^{}]*\"id\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"name\"\\s*:\\s*\"([^\"]*)\"[^{}]*\"mode\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"world\"\\s*:\\s*\"([^\"]+)\"",
                Pattern.DOTALL).matcher(block);
        while (matcher.find()) {
            String id = matcher.group(1);
            String name = matcher.group(2);
            String mode = matcher.group(3);
            String worldName = matcher.group(4);
            String chunk = extractObject(block, id);
            if (chunk == null) {
                continue;
            }
            ArenaDefinition def = parseArenaObject(id, name, mode, worldName, chunk);
            if (def != null) {
                result.add(def);
            }
        }
        return result;
    }

    static ArenaDefinition parseSingle(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        String id = extractString(json, "id");
        if (id == null) {
            return null;
        }
        return parseArenaObject(
                id,
                extractString(json, "name"),
                extractString(json, "mode"),
                extractString(json, "world"),
                json);
    }

    private static ArenaDefinition parseArenaObject(
            String id, String name, String mode, String worldName, String chunk) {
        World world = Bukkit.getWorld(worldName);
        if (world == null && worldName != null) {
            world = Bukkit.getWorlds().stream()
                    .filter(w -> w.getName().equalsIgnoreCase(worldName))
                    .findFirst()
                    .orElse(null);
        }
        if (world == null) {
            return null;
        }
        Location pos1 = readPos(chunk, "pos1");
        if (pos1 == null) {
            pos1 = readPos(chunk, "pos1_spawn");
        }
        Location pos2 = readPos(chunk, "pos2");
        if (pos2 == null) {
            pos2 = readPos(chunk, "pos2_spawn");
        }
        if (pos1 == null || pos2 == null) {
            return null;
        }
        pos1.setWorld(world);
        pos2.setWorld(world);
        boolean enabled = !chunk.contains("\"enabled\":false") && !chunk.contains("\"enabled\": false");
        boolean autoReset = chunk.contains("\"auto_reset\":true") || chunk.contains("\"auto_reset\": true");
        return ArenaDefinition.fromBackend(id, name, mode, world.getName(), pos1, pos2, enabled, autoReset);
    }

    private static Location readPos(String chunk, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\\{([^{}]*)\\}").matcher(chunk);
        if (!m.find()) {
            return null;
        }
        String inner = m.group(1);
        Double x = extractDouble(inner, "x");
        Double y = extractDouble(inner, "y");
        Double z = extractDouble(inner, "z");
        if (x == null || y == null || z == null) {
            return null;
        }
        float yaw = extractFloat(inner, "yaw", 0f);
        float pitch = extractFloat(inner, "pitch", 0f);
        return new Location(null, x, y, z, yaw, pitch);
    }

    private static String extractObject(String block, String id) {
        int idx = block.indexOf("\"id\"");
        while (idx >= 0) {
            int start = block.lastIndexOf('{', idx);
            if (start < 0) {
                break;
            }
            int depth = 0;
            for (int i = start; i < block.length(); i++) {
                char c = block.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        String obj = block.substring(start, i + 1);
                        if (obj.contains("\"id\"") && obj.contains(id)) {
                            return obj;
                        }
                        break;
                    }
                }
            }
            idx = block.indexOf("\"id\"", idx + 4);
        }
        return null;
    }

    private static String extractString(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static Double extractDouble(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : null;
    }

    private static float extractFloat(String json, String key, float def) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
        return m.find() ? Float.parseFloat(m.group(1)) : def;
    }
}

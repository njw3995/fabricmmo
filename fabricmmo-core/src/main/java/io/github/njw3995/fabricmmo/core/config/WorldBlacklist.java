package io.github.njw3995.fabricmmo.core.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/** Case-insensitive world-name blacklist matching mcMMO's world_blacklist.txt behavior. */
public final class WorldBlacklist {
    private final Set<String> configuredNames;
    private final String levelName;
    private final Map<String, Boolean> resolvedWorldIds = new HashMap<>();

    private WorldBlacklist(Set<String> configuredNames, String levelName) {
        this.configuredNames = Set.copyOf(configuredNames);
        this.levelName = levelName;
    }

    public static WorldBlacklist load(Path file, Path worldRoot) throws IOException {
        if (!Files.exists(file)) {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.createFile(file);
        }
        Set<String> names = new HashSet<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            // Upstream deliberately does not trim lines or treat comments specially.
            if (!line.isEmpty()) {
                names.add(line.toLowerCase(Locale.ROOT));
            }
        }
        Path fileName = worldRoot.getFileName();
        String levelName = fileName == null ? "world" : fileName.toString();
        return new WorldBlacklist(names, levelName);
    }

    public synchronized boolean isBlacklisted(ServerWorld world) {
        String worldId = world.getRegistryKey().getValue().toString();
        return resolvedWorldIds.computeIfAbsent(
                worldId, ignored -> configuredNames.contains(worldName(world.getRegistryKey())
                        .toLowerCase(Locale.ROOT)));
    }

    /** Tests a canonical FabricMMO world name using upstream's case-insensitive comparison. */
    public synchronized boolean isBlacklisted(String worldName) {
        return configuredNames.contains(worldName.toLowerCase(Locale.ROOT));
    }

    public synchronized void register(ServerWorld world) {
        String worldId = world.getRegistryKey().getValue().toString();
        resolvedWorldIds.put(
                worldId,
                configuredNames.contains(worldName(world.getRegistryKey())
                        .toLowerCase(Locale.ROOT)));
    }

    public synchronized void unregister(ServerWorld world) {
        resolvedWorldIds.remove(world.getRegistryKey().getValue().toString());
    }

    String worldName(RegistryKey<World> key) {
        if (key.equals(World.OVERWORLD)) {
            return levelName;
        }
        if (key.equals(World.NETHER)) {
            return levelName + "_nether";
        }
        if (key.equals(World.END)) {
            return levelName + "_the_end";
        }
        // Bukkit exposes a single world name. For Fabric custom dimensions, the stable
        // equivalent is the complete namespaced dimension id, never its ambiguous bare path.
        return key.getValue().toString();
    }
}

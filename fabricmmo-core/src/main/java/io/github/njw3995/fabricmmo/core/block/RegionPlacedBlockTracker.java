package io.github.njw3995.fabricmmo.core.block;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * mcMMO-style placed-block storage: cached bitset chunks grouped into 32x32 region files.
 */
public final class RegionPlacedBlockTracker implements PlacedBlockTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/BlockTracker");
    private static final int LEGACY_MAGIC = 0x464D5042; // FMPB
    private static final int LEGACY_VERSION = 1;

    private final Path legacyRoot;
    private final Map<String, TrackedWorld> worlds = new HashMap<>();
    private final Map<ChunkKey, BitSetChunkStore> chunks = new HashMap<>();
    private final Map<RegionKey, PlacedBlockRegionFile> regions = new HashMap<>();
    private final Map<RegionKey, Set<ChunkKey>> regionUsage = new HashMap<>();
    private boolean closed;

    public RegionPlacedBlockTracker(Path legacyRoot) {
        this.legacyRoot = legacyRoot.toAbsolutePath().normalize();
    }

    @Override
    public synchronized void registerWorld(TrackedWorld world) throws IOException {
        requireOpen();
        TrackedWorld existing = worlds.get(world.worldId());
        if (world.equals(existing)) {
            return;
        }
        if (existing != null) {
            unloadWorld(world.worldId());
        }
        Files.createDirectories(world.regionDirectory());
        worlds.put(world.worldId(), world);
    }

    @Override
    public synchronized boolean isIneligible(BlockLocation location) {
        requireOpen();
        try {
            TrackedWorld world = world(location.worldId());
            return getOrLoad(world, ChunkKey.from(location)).isIneligible(
                    location.x(), location.y(), location.z());
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to check placed-block marker at {}", location, exception);
            return false;
        }
    }

    @Override
    public synchronized void setIneligible(BlockLocation location) {
        set(location, true);
    }

    @Override
    public synchronized void setEligible(BlockLocation location) {
        set(location, false);
    }

    private void set(BlockLocation location, boolean value) {
        requireOpen();
        try {
            TrackedWorld world = world(location.worldId());
            getOrLoad(world, ChunkKey.from(location)).set(
                    location.x(), location.y(), location.z(), value);
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to update placed-block marker at {}", location, exception);
        }
    }

    @Override
    public synchronized void chunkUnloaded(String worldId, int chunkX, int chunkZ) {
        if (closed) {
            return;
        }
        TrackedWorld world = worlds.get(worldId);
        if (world == null) {
            return;
        }
        ChunkKey chunkKey = new ChunkKey(worldId, chunkX, chunkZ);
        BitSetChunkStore store = chunks.remove(chunkKey);
        try {
            if (store != null && store.isDirty()) {
                write(world, store);
            }
        } catch (Exception exception) {
            logSaveFailure(worldId, chunkX, chunkZ, exception);
        } finally {
            releaseRegion(chunkKey);
        }
    }

    @Override
    public synchronized void unloadWorld(String worldId) {
        if (closed) {
            return;
        }
        TrackedWorld world = worlds.get(worldId);
        if (world == null) {
            return;
        }
        List<ChunkKey> worldChunks = chunks.keySet().stream()
                .filter(key -> key.worldId().equals(worldId))
                .toList();
        for (ChunkKey key : worldChunks) {
            BitSetChunkStore store = chunks.remove(key);
            if (store != null && store.isDirty()) {
                try {
                    write(world, store);
                } catch (Exception exception) {
                    logSaveFailure(worldId, key.chunkX(), key.chunkZ(), exception);
                }
            }
        }
        List<RegionKey> worldRegions = regions.keySet().stream()
                .filter(key -> key.worldId().equals(worldId))
                .toList();
        for (RegionKey key : worldRegions) {
            closeQuietly(regions.remove(key));
            regionUsage.remove(key);
        }
        worlds.remove(worldId);
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        List<String> worldIds = new ArrayList<>(worlds.keySet());
        for (String worldId : worldIds) {
            unloadWorld(worldId);
        }
        chunks.clear();
        regionUsage.clear();
        regions.values().forEach(RegionPlacedBlockTracker::closeQuietly);
        regions.clear();
        closed = true;
    }

    private BitSetChunkStore getOrLoad(TrackedWorld world, ChunkKey key) {
        return chunks.computeIfAbsent(key, ignored -> {
            RegionKey regionKey = RegionKey.from(key);
            regionUsage.computeIfAbsent(regionKey, unused -> new HashSet<>()).add(key);
            BitSetChunkStore loaded = load(world, key);
            return loaded == null ? new BitSetChunkStore(world, key.chunkX(), key.chunkZ()) : loaded;
        });
    }

    private BitSetChunkStore load(TrackedWorld world, ChunkKey key) {
        try {
            byte[] data = region(world, RegionKey.from(key)).read(key.chunkX(), key.chunkZ());
            if (data != null) {
                BitSetChunkStore store = BitSetChunkStore.deserialize(data, world);
                if (store.chunkX() != key.chunkX() || store.chunkZ() != key.chunkZ()) {
                    throw new IOException("Placed-block chunk identity mismatch");
                }
                return store;
            }
        } catch (Exception exception) {
            LOGGER.warn(
                    "Failed to read placed-block data for chunk ({}, {}) in '{}'; treating it as empty",
                    key.chunkX(), key.chunkZ(), key.worldId(), exception);
            return null;
        }

        BitSetChunkStore legacy;
        try {
            legacy = loadLegacy(world, key);
        } catch (Exception exception) {
            LOGGER.warn(
                    "Failed to read legacy placed-block data for chunk ({}, {}) in '{}'; treating it as empty",
                    key.chunkX(), key.chunkZ(), key.worldId(), exception);
            return null;
        }
        if (legacy == null) {
            return null;
        }

        try {
            write(world, legacy);
            deleteLegacy(key);
        } catch (Exception exception) {
            LOGGER.warn(
                    "Unable to migrate legacy placed-block data for chunk ({}, {}) in '{}'; retaining it for retry",
                    key.chunkX(), key.chunkZ(), key.worldId(), exception);
        }
        return legacy;
    }

    private void write(TrackedWorld world, BitSetChunkStore store) throws IOException {
        PlacedBlockRegionFile region = region(world,
                new RegionKey(store.worldId(), store.chunkX() >> 5, store.chunkZ() >> 5));
        region.write(store.chunkX(), store.chunkZ(), store.isEmpty() ? null : store.serialize());
        store.markClean();
    }

    private PlacedBlockRegionFile region(TrackedWorld world, RegionKey key) throws IOException {
        PlacedBlockRegionFile existing = regions.get(key);
        if (existing != null) {
            return existing;
        }
        Path path = world.regionDirectory().resolve(
                "fabricmmo_" + key.regionX() + '_' + key.regionZ() + "_.fmr");
        PlacedBlockRegionFile opened = new PlacedBlockRegionFile(path);
        regions.put(key, opened);
        return opened;
    }

    private void releaseRegion(ChunkKey chunkKey) {
        RegionKey regionKey = RegionKey.from(chunkKey);
        Set<ChunkKey> usage = regionUsage.get(regionKey);
        if (usage == null) {
            return;
        }
        usage.remove(chunkKey);
        if (!usage.isEmpty()) {
            return;
        }
        regionUsage.remove(regionKey);
        closeQuietly(regions.remove(regionKey));
    }

    private BitSetChunkStore loadLegacy(TrackedWorld world, ChunkKey key) throws IOException {
        Path path = legacyPath(key);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        BitSetChunkStore store = new BitSetChunkStore(world, key.chunkX(), key.chunkZ());
        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            int magic = input.readInt();
            int version = input.readInt();
            String worldId = input.readUTF();
            int chunkX = input.readInt();
            int chunkZ = input.readInt();
            int count = input.readInt();
            if (magic != LEGACY_MAGIC || version != LEGACY_VERSION
                    || !worldId.equals(key.worldId())
                    || chunkX != key.chunkX()
                    || chunkZ != key.chunkZ()
                    || count < 0) {
                throw new IOException("Invalid legacy placed-block file: " + path);
            }
            for (int index = 0; index < count; index++) {
                int x = input.readInt();
                int y = input.readInt();
                int z = input.readInt();
                store.set(x, y, z, true);
            }
            if (input.read() != -1) {
                throw new IOException("Trailing data in legacy placed-block file: " + path);
            }
            return store;
        } catch (EOFException exception) {
            throw new IOException("Truncated legacy placed-block file: " + path, exception);
        }
    }

    private void deleteLegacy(ChunkKey key) {
        Path path = legacyPath(key);
        try {
            Files.deleteIfExists(path);
            Path parent = path.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                try (var entries = Files.list(parent)) {
                    if (entries.findAny().isEmpty()) {
                        Files.deleteIfExists(parent);
                    }
                }
            }
        } catch (IOException exception) {
            LOGGER.warn("Unable to remove migrated legacy placed-block file {}", path, exception);
        }
    }

    private Path legacyPath(ChunkKey key) {
        String encodedWorld = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(key.worldId().getBytes(StandardCharsets.UTF_8));
        return legacyRoot.resolve(encodedWorld)
                .resolve("c." + key.chunkX() + '.' + key.chunkZ() + ".fmpb");
    }

    private TrackedWorld world(String worldId) {
        TrackedWorld world = worlds.get(worldId);
        if (world == null) {
            throw new IllegalStateException("Placed-block world is not registered: " + worldId);
        }
        return world;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Placed-block tracker is closed");
        }
    }

    private static void logSaveFailure(String worldId, int chunkX, int chunkZ, Exception exception) {
        LOGGER.warn("Failed to save placed-block data for chunk ({}, {}) in '{}'",
                chunkX, chunkZ, worldId, exception);
    }

    private static void closeQuietly(PlacedBlockRegionFile region) {
        if (region == null) {
            return;
        }
        try {
            region.close();
        } catch (IOException exception) {
            LOGGER.warn("Failed to close placed-block region file", exception);
        }
    }

    private record ChunkKey(String worldId, int chunkX, int chunkZ) {
        static ChunkKey from(BlockLocation location) {
            return new ChunkKey(location.worldId(), location.chunkX(), location.chunkZ());
        }
    }

    private record RegionKey(String worldId, int regionX, int regionZ) {
        static RegionKey from(ChunkKey chunk) {
            return new RegionKey(chunk.worldId(), chunk.chunkX() >> 5, chunk.chunkZ() >> 5);
        }
    }
}

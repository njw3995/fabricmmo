package io.github.njw3995.fabricmmo.core.block;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Persists player-created block markers in independent chunk files.
 */
public final class ChunkPlacedBlockTracker implements PlacedBlockTracker {
    private static final int MAGIC = 0x464D5042; // FMPB
    private static final int FORMAT_VERSION = 1;
    private static final int MAX_CACHED_CHUNKS = 1024;

    private final Path rootDirectory;
    private final Map<ChunkKey, NavigableSet<BlockLocation>> loadedChunks =
            new LinkedHashMap<>(16, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<ChunkKey, NavigableSet<BlockLocation>> eldest) {
                    return size() > MAX_CACHED_CHUNKS;
                }
            };
    private boolean closed;

    public ChunkPlacedBlockTracker(Path rootDirectory) throws IOException {
        this.rootDirectory = rootDirectory.toAbsolutePath().normalize();
        Files.createDirectories(this.rootDirectory);
    }

    @Override
    public synchronized boolean isPlaced(BlockLocation location) throws IOException {
        requireOpen();
        return loadChunk(ChunkKey.from(location)).contains(location);
    }

    @Override
    public synchronized void markPlaced(BlockLocation location) throws IOException {
        requireOpen();
        ChunkKey key = ChunkKey.from(location);
        NavigableSet<BlockLocation> locations = loadChunk(key);
        if (locations.add(location)) {
            writeChunk(key, locations);
        }
    }

    @Override
    public synchronized void clear(BlockLocation location) throws IOException {
        requireOpen();
        ChunkKey key = ChunkKey.from(location);
        NavigableSet<BlockLocation> locations = loadChunk(key);
        if (locations.remove(location)) {
            writeChunk(key, locations);
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        loadedChunks.clear();
    }

    private NavigableSet<BlockLocation> loadChunk(ChunkKey key) throws IOException {
        NavigableSet<BlockLocation> existing = loadedChunks.get(key);
        if (existing != null) {
            return existing;
        }

        NavigableSet<BlockLocation> loaded = new TreeSet<>();
        Path path = chunkPath(key);
        if (Files.isRegularFile(path)) {
            try (DataInputStream input = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(path)))) {
                int magic = input.readInt();
                int version = input.readInt();
                String worldId = input.readUTF();
                int chunkX = input.readInt();
                int chunkZ = input.readInt();
                int count = input.readInt();
                if (magic != MAGIC) {
                    throw new IOException("Invalid placed-block file magic: " + path);
                }
                if (version != FORMAT_VERSION) {
                    throw new IOException("Unsupported placed-block format " + version + ": " + path);
                }
                if (!worldId.equals(key.worldId()) || chunkX != key.chunkX() || chunkZ != key.chunkZ()) {
                    throw new IOException("Placed-block file identity mismatch: " + path);
                }
                if (count < 0) {
                    throw new IOException("Negative placed-block entry count: " + path);
                }
                for (int index = 0; index < count; index++) {
                    BlockLocation location = new BlockLocation(
                            worldId,
                            input.readInt(),
                            input.readInt(),
                            input.readInt());
                    if (location.chunkX() != chunkX || location.chunkZ() != chunkZ) {
                        throw new IOException("Placed-block entry belongs to another chunk: " + path);
                    }
                    loaded.add(location);
                }
                if (input.read() != -1) {
                    throw new IOException("Trailing data in placed-block file: " + path);
                }
            } catch (EOFException exception) {
                throw new IOException("Truncated placed-block file: " + path, exception);
            }
        }
        loadedChunks.put(key, loaded);
        return loaded;
    }

    private void writeChunk(ChunkKey key, NavigableSet<BlockLocation> locations) throws IOException {
        Path target = chunkPath(key);
        Files.createDirectories(target.getParent());
        if (locations.isEmpty()) {
            Files.deleteIfExists(target);
            return;
        }

        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try (DataOutputStream output = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(temporary)))) {
            output.writeInt(MAGIC);
            output.writeInt(FORMAT_VERSION);
            output.writeUTF(key.worldId());
            output.writeInt(key.chunkX());
            output.writeInt(key.chunkZ());
            output.writeInt(locations.size());
            for (BlockLocation location : locations) {
                output.writeInt(location.x());
                output.writeInt(location.y());
                output.writeInt(location.z());
            }
        }
        moveAtomically(temporary, target);
    }

    private Path chunkPath(ChunkKey key) {
        String encodedWorld = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(key.worldId().getBytes(StandardCharsets.UTF_8));
        return rootDirectory.resolve(encodedWorld)
                .resolve("c." + key.chunkX() + '.' + key.chunkZ() + ".fmpb");
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Placed-block tracker is closed");
        }
    }

    private record ChunkKey(String worldId, int chunkX, int chunkZ) {
        private static ChunkKey from(BlockLocation location) {
            return new ChunkKey(location.worldId(), location.chunkX(), location.chunkZ());
        }
    }
}

package io.github.njw3995.fabricmmo.core.block;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * Backward-compatible standalone wrapper retained for API consumers and unit tests.
 * Production runtime uses {@link RegionPlacedBlockTracker} with real dimension bounds.
 */
public final class ChunkPlacedBlockTracker implements PlacedBlockTracker {
    private static final int DEFAULT_MINIMUM_Y = -64;
    private static final int DEFAULT_MAXIMUM_Y_EXCLUSIVE = 320;

    private final Path rootDirectory;
    private final RegionPlacedBlockTracker delegate;
    private final Set<String> registeredWorlds = new HashSet<>();

    public ChunkPlacedBlockTracker(Path rootDirectory) {
        this.rootDirectory = rootDirectory.toAbsolutePath().normalize();
        delegate = new RegionPlacedBlockTracker(rootDirectory);
    }

    @Override
    public synchronized boolean isIneligible(BlockLocation location) {
        ensureWorld(location.worldId());
        return delegate.isIneligible(location);
    }

    @Override
    public synchronized void setIneligible(BlockLocation location) {
        ensureWorld(location.worldId());
        delegate.setIneligible(location);
    }

    @Override
    public synchronized void setEligible(BlockLocation location) {
        ensureWorld(location.worldId());
        delegate.setEligible(location);
    }

    @Override
    public synchronized void registerWorld(TrackedWorld world) throws IOException {
        delegate.registerWorld(world);
        registeredWorlds.add(world.worldId());
    }

    @Override
    public synchronized void chunkUnloaded(String worldId, int chunkX, int chunkZ) {
        delegate.chunkUnloaded(worldId, chunkX, chunkZ);
    }

    @Override
    public synchronized void unloadWorld(String worldId) {
        delegate.unloadWorld(worldId);
        registeredWorlds.remove(worldId);
    }

    @Override
    public synchronized void close() {
        delegate.close();
        registeredWorlds.clear();
    }

    private void ensureWorld(String worldId) {
        if (registeredWorlds.contains(worldId)) {
            return;
        }
        String encodedWorld = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(worldId.getBytes(StandardCharsets.UTF_8));
        try {
            registerWorld(new TrackedWorld(
                    worldId,
                    rootDirectory.resolve(encodedWorld),
                    DEFAULT_MINIMUM_Y,
                    DEFAULT_MAXIMUM_Y_EXCLUSIVE));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to register placed-block test world", exception);
        }
    }
}

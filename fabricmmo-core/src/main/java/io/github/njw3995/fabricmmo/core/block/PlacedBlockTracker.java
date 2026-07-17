package io.github.njw3995.fabricmmo.core.block;

import java.io.IOException;

/**
 * Location-based natural-block eligibility tracker, equivalent to mcMMO's UserBlockTracker.
 */
public interface PlacedBlockTracker extends AutoCloseable {
    boolean isIneligible(BlockLocation location);

    default boolean isEligible(BlockLocation location) {
        return !isIneligible(location);
    }

    void setIneligible(BlockLocation location);

    void setEligible(BlockLocation location);

    void registerWorld(TrackedWorld world) throws IOException;

    void chunkUnloaded(String worldId, int chunkX, int chunkZ);

    void unloadWorld(String worldId);

    @Override
    void close();

    /** Compatibility names used by the initial FabricMMO Mining implementation. */
    default boolean isPlaced(BlockLocation location) {
        return isIneligible(location);
    }

    default void markPlaced(BlockLocation location) {
        setIneligible(location);
    }

    default void clear(BlockLocation location) {
        setEligible(location);
    }
}

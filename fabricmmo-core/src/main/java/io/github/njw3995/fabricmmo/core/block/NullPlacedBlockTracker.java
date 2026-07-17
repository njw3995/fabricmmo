package io.github.njw3995.fabricmmo.core.block;

public final class NullPlacedBlockTracker implements PlacedBlockTracker {
    @Override
    public boolean isIneligible(BlockLocation location) {
        return false;
    }

    @Override
    public void setIneligible(BlockLocation location) {
    }

    @Override
    public void setEligible(BlockLocation location) {
    }

    @Override
    public void registerWorld(TrackedWorld world) {
    }

    @Override
    public void chunkUnloaded(String worldId, int chunkX, int chunkZ) {
    }

    @Override
    public void unloadWorld(String worldId) {
    }

    @Override
    public void close() {
    }
}

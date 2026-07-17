package io.github.njw3995.fabricmmo.core.block;

import java.util.Objects;

public record BlockLocation(String worldId, int x, int y, int z)
        implements Comparable<BlockLocation> {
    public BlockLocation {
        worldId = Objects.requireNonNull(worldId, "worldId").trim();
        if (worldId.isEmpty()) {
            throw new IllegalArgumentException("worldId must not be blank");
        }
    }

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }

    @Override
    public int compareTo(BlockLocation other) {
        int worldComparison = worldId.compareTo(other.worldId);
        if (worldComparison != 0) {
            return worldComparison;
        }
        int xComparison = Integer.compare(x, other.x);
        if (xComparison != 0) {
            return xComparison;
        }
        int yComparison = Integer.compare(y, other.y);
        if (yComparison != 0) {
            return yComparison;
        }
        return Integer.compare(z, other.z);
    }
}

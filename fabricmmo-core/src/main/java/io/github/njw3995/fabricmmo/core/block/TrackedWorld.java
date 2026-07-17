package io.github.njw3995.fabricmmo.core.block;

import java.nio.file.Path;
import java.util.Objects;

public record TrackedWorld(
        String worldId,
        Path regionDirectory,
        int minimumY,
        int maximumYExclusive) {
    public TrackedWorld {
        worldId = Objects.requireNonNull(worldId, "worldId").trim();
        regionDirectory = Objects.requireNonNull(regionDirectory, "regionDirectory")
                .toAbsolutePath()
                .normalize();
        if (worldId.isEmpty()) {
            throw new IllegalArgumentException("worldId must not be blank");
        }
        if (maximumYExclusive <= minimumY) {
            throw new IllegalArgumentException("maximumYExclusive must exceed minimumY");
        }
    }
}

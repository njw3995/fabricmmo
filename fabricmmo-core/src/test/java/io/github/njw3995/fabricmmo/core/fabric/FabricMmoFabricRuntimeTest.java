package io.github.njw3995.fabricmmo.core.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FabricMmoFabricRuntimeTest {
    @Test
    void normalizesWorldScopedDataDirectories() {
        Path worldRoot = Path.of("world").resolve(".");

        Path players = FabricMmoFabricRuntime.resolvePlayerDataDirectory(worldRoot);
        Path placedBlocks = FabricMmoFabricRuntime.resolvePlacedBlockDirectory(worldRoot);

        assertTrue(players.isAbsolute());
        assertTrue(placedBlocks.isAbsolute());
        assertEquals(
                Path.of("world", "fabricmmo", "players").toAbsolutePath().normalize(),
                players);
        assertEquals(
                Path.of("world", "fabricmmo", "placed-blocks").toAbsolutePath().normalize(),
                placedBlocks);
    }
}

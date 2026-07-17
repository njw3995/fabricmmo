package io.github.njw3995.fabricmmo.core.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FabricMmoFabricRuntimeTest {
    @Test
    void normalizesPlayerDataDirectory() {
        Path resolved = FabricMmoFabricRuntime.resolvePlayerDataDirectory(
                Path.of("world").resolve("."));

        assertTrue(resolved.isAbsolute());
        assertEquals(
                Path.of("world", "fabricmmo", "players").toAbsolutePath().normalize(),
                resolved);
    }
}

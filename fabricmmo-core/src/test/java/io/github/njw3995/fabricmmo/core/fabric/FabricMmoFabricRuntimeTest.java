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
        Path miningAbilities = FabricMmoFabricRuntime.resolveMiningAbilityDirectory(worldRoot);
        Path woodcuttingAbilities =
                FabricMmoFabricRuntime.resolveWoodcuttingAbilityDirectory(worldRoot);
        Path excavationAbilities =
                FabricMmoFabricRuntime.resolveExcavationAbilityDirectory(worldRoot);

        assertTrue(players.isAbsolute());
        assertTrue(placedBlocks.isAbsolute());
        assertTrue(miningAbilities.isAbsolute());
        assertTrue(woodcuttingAbilities.isAbsolute());
        assertTrue(excavationAbilities.isAbsolute());
        assertEquals(
                Path.of("world", "fabricmmo", "players").toAbsolutePath().normalize(),
                players);
        assertEquals(
                Path.of("world", "fabricmmo", "placed-blocks").toAbsolutePath().normalize(),
                placedBlocks);
        assertEquals(
                Path.of("world", "fabricmmo", "mining-abilities").toAbsolutePath().normalize(),
                miningAbilities);
        assertEquals(
                Path.of("world", "fabricmmo", "woodcutting-abilities")
                        .toAbsolutePath().normalize(),
                woodcuttingAbilities);
        assertEquals(
                Path.of("world", "fabricmmo", "excavation-abilities")
                        .toAbsolutePath().normalize(),
                excavationAbilities);
    }
}

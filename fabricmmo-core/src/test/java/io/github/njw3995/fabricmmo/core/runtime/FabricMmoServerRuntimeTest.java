package io.github.njw3995.fabricmmo.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FabricMmoServerRuntimeTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preservesProgressionAcrossServerRuntimeRestart() throws Exception {
        UUID playerId = UUID.randomUUID();
        Path playerDataDirectory = temporaryDirectory.resolve("players");

        try (FabricMmoServerRuntime first = FabricMmoServerRuntime.start(
                playerDataDirectory, ignored -> { })) {
            first.api().progression().award(new XpAwardRequest(
                    playerId,
                    CoreSkills.MINING,
                    CoreXpSources.MINING_BLOCK_BREAK,
                    1020,
                    Map.of("test", "restart")));
            assertEquals(1, first.api().progression().query(playerId, CoreSkills.MINING).level());
        }

        try (FabricMmoServerRuntime second = FabricMmoServerRuntime.start(
                playerDataDirectory, ignored -> { })) {
            assertEquals(1, second.api().progression().query(playerId, CoreSkills.MINING).level());
        }
    }

    @Test
    void persistsEachAwardBeforeGracefulShutdown() throws Exception {
        UUID playerId = UUID.randomUUID();
        Path playerDataDirectory = temporaryDirectory.resolve("crash-durable");
        FabricMmoServerRuntime first = FabricMmoServerRuntime.start(
                playerDataDirectory, ignored -> { });
        try {
            first.api().progression().award(new XpAwardRequest(
                    playerId,
                    CoreSkills.MINING,
                    CoreXpSources.MINING_BLOCK_BREAK,
                    1020,
                    Map.of("test", "crash-durable")));

            assertTrue(java.nio.file.Files.exists(
                    playerDataDirectory.resolve(playerId + ".properties")));
            try (FabricMmoServerRuntime second = FabricMmoServerRuntime.start(
                    playerDataDirectory, ignored -> { })) {
                assertEquals(1,
                        second.api().progression().query(playerId, CoreSkills.MINING).level());
            }
        } finally {
            first.close();
        }
    }

    @Test
    void rejectsApiAccessAfterClose() throws Exception {
        FabricMmoServerRuntime runtime = FabricMmoServerRuntime.start(
                temporaryDirectory.resolve("closed"), ignored -> { });
        runtime.close();
        assertThrows(IllegalStateException.class, runtime::api);
    }
}

package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PropertiesMiningAbilityStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesAndReloadsBothCooldownTimestamps() throws Exception {
        UUID playerId = UUID.randomUUID();
        MiningAbilityData expected = new MiningAbilityData(12_345L, 67_890L);
        try (PropertiesMiningAbilityStore store =
                     new PropertiesMiningAbilityStore(temporaryDirectory)) {
            store.save(playerId, expected);
        }
        try (PropertiesMiningAbilityStore reopened =
                     new PropertiesMiningAbilityStore(temporaryDirectory)) {
            assertEquals(expected, reopened.load(playerId));
        }
    }
}

package io.github.njw3995.fabricmmo.core.skill.taming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TamingDefaultResourcesTest {
    @TempDir Path temp;

    @Test
    void generatedDefaultsExposePinnedTamingConfiguration() throws Exception {
        Path config = copyResource("defaults/config.yml");
        Path advanced = copyResource("defaults/advanced.yml");
        Path ranks = copyResource("defaults/skillranks.yml");
        Path experience = copyResource("defaults/experience.yml");

        TamingSettings settings = TamingSettings.load(
                config, advanced, ranks, experience, ProgressionMode.RETRO);
        assertTrue(settings.pvpEnabled());
        assertTrue(settings.pveEnabled());
        assertEquals(2.0D, settings.goreModifier());
        assertEquals(50.0D, settings.fastFoodChance());
        assertEquals(6.0D, settings.shockProofModifier());
        assertEquals("minecraft:bone",
                settings.summons().get(TamingSummonType.WOLF).itemId().toString());
        assertEquals(240,
                settings.summons().get(TamingSummonType.HORSE).summonLengthSeconds());

        TamingXpTable xp = TamingXpTable.load(experience);
        assertEquals(250.0D, xp.xp("wolf"));
        assertEquals(1500.0D, xp.xp("sniffer"));
        assertEquals(900.0D, xp.xp("frog"));
    }

    private Path copyResource(String resource) throws IOException {
        Path target = temp.resolve(Path.of(resource).getFileName().toString());
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing test resource: " + resource);
            }
            Files.copy(input, target);
        }
        return target;
    }
}

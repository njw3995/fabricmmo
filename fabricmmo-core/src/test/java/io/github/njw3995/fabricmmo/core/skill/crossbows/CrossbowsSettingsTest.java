package io.github.njw3995.fabricmmo.core.skill.crossbows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CrossbowsSettingsTest {
    @Test
    void loadsPinnedUpstreamDefaults() throws Exception {
        CrossbowsSettings settings = settings();

        assertEquals(ProgressionMode.RETRO, settings.progressionMode());
        assertTrue(settings.pvpEnabled());
        assertTrue(settings.pveEnabled());
        assertFalse(settings.limitBreakPve());
        assertEquals(1, settings.poweredShotRank(1));
        assertEquals(20, settings.poweredShotRank(1000));
        assertEquals(1, settings.trickShotRank(50));
        assertEquals(2, settings.trickShotRank(200));
        assertEquals(3, settings.trickShotRank(400));
        assertEquals(19.0D, settings.poweredShotDamage(10.0D, 1000), 1.0E-9);
        assertEquals(2.25D, settings.distanceMultiplier(50.0D), 1.0E-9);
    }

    private static CrossbowsSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return CrossbowsSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("experience.yml"));
    }
}

package io.github.njw3995.fabricmmo.core.skill.archery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ArcherySettingsTest {
    @Test
    void loadsPinnedUpstreamDefaults() throws Exception {
        ArcherySettings settings = settings();

        assertEquals(ProgressionMode.RETRO, settings.progressionMode());
        assertTrue(settings.pvpEnabled());
        assertTrue(settings.pveEnabled());
        assertFalse(settings.limitBreakPve());
        assertEquals(2.0D, settings.forceMultiplier(), 1.0E-9);
        assertEquals(0.025D, settings.distanceXpMultiplier(), 1.0E-9);
        assertEquals(1, settings.skillShotRank(1));
        assertEquals(20, settings.skillShotRank(1000));
        assertEquals(10, settings.limitBreakRank(1000));
        assertEquals(200.0D, settings.skillShotBonusPercent(1000) * 100.0D, 1.0E-9);
        assertEquals(19.0D, settings.skillShotDamage(10.0D, 1000), 1.0E-9);
        assertEquals(50.0D, settings.dazeChancePercent(1000, false), 1.0E-9);
        assertEquals(100.0D, settings.retrievalChancePercent(1000, false), 1.0E-9);
        assertEquals(2.25D, settings.distanceMultiplier(50.0D), 1.0E-9);
        assertEquals(2.25D, settings.distanceMultiplier(100.0D), 1.0E-9);
    }

    private static ArcherySettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return ArcherySettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("experience.yml"));
    }
}

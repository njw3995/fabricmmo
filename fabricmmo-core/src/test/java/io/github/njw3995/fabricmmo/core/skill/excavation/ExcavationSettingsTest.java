package io.github.njw3995.fabricmmo.core.skill.excavation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ExcavationSettingsTest {
    @Test
    void loadsPinnedUpstreamDefaults() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        ExcavationSettings settings = ExcavationSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"));

        assertTrue(settings.abilitiesEnabled());
        assertEquals(240, settings.gigaDrillCooldownSeconds());
        assertEquals(50, settings.gigaDrillUnlockLevel());
        assertEquals(3, settings.gigaDrillDurationSeconds(50));
        assertEquals(8, settings.archaeologyRank(1000));
        assertEquals(16.0D, settings.archaeologyOrbChancePercent(1000));
        assertEquals(8, settings.archaeologyOrbAmount(1000));
    }
}

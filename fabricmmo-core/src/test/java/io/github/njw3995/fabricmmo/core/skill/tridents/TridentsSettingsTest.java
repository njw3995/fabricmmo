package io.github.njw3995.fabricmmo.core.skill.tridents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TridentsSettingsTest {
    @Test
    void loadsPinnedUpstreamDefaultsAndFixedImpaleFormula() throws Exception {
        TridentsSettings settings = settings();

        assertEquals(ProgressionMode.RETRO, settings.progressionMode());
        assertTrue(settings.pvpEnabled());
        assertTrue(settings.pveEnabled());
        assertTrue(settings.adjustForAttackCooldown());
        assertFalse(settings.limitBreakPve());
        assertEquals(1, settings.impaleRank(50));
        assertEquals(10, settings.impaleRank(1000));
        assertEquals(1.5D, settings.impaleDamage(50), 1.0E-9);
        assertEquals(6.0D, settings.impaleDamage(1000), 1.0E-9);
        assertEquals(10, settings.limitBreakRank(1000));
    }

    private static TridentsSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return TridentsSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"));
    }
}

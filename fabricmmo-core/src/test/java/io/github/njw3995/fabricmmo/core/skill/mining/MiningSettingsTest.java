package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MiningSettingsTest {
    @Test
    void loadsPinnedUpstreamMiningDefaults() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        MiningSettings settings = MiningSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("experience.yml"));

        assertEquals(ProgressionMode.RETRO, settings.progressionMode());
        assertEquals(50, settings.superBreakerUnlockLevel());
        assertEquals(3, settings.superBreakerDurationSeconds(50));
        assertEquals(0, settings.blastRank(99));
        assertEquals(1, settings.blastRank(100));
        assertEquals(8, settings.blastRank(1000));
        assertEquals(2, settings.dropMultiplier(3));
        assertEquals(3, settings.dropMultiplier(8));
        assertEquals(4.0D, settings.blastRadiusModifier(8));
        assertEquals(100.0D, settings.blastDamageDecreasePercent(8));
        assertTrue(settings.pistonExploitPrevention());
        assertTrue(settings.lavaStoneExploitPrevention());
    }
}

package io.github.njw3995.fabricmmo.core.skill.smelting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SmeltingSettingsTest {
    private static final Path DEFAULTS = Path.of("src/main/resources/defaults");

    @Test
    void pinnedDefaultsLoadWithRetroRanksAndMinecraft1211Values() throws Exception {
        SmeltingSettings settings = SmeltingSettings.load(
                DEFAULTS.resolve("config.yml"),
                DEFAULTS.resolve("advanced.yml"),
                DEFAULTS.resolve("skillranks.yml"),
                DEFAULTS.resolve("experience.yml"));

        assertEquals(ProgressionMode.RETRO, settings.progressionMode());
        assertEquals(0, settings.fuelEfficiencyRank(99));
        assertEquals(1, settings.fuelEfficiencyRank(100));
        assertEquals(3, settings.fuelEfficiencyRank(750));
        assertEquals(8, settings.understandingRank(1000));
        assertEquals(50.0D, settings.secondSmeltChance(1000, false));
        assertEquals(66.65D, settings.secondSmeltChance(1000, true), 0.0001D);
        assertEquals(25, settings.xpForInput("minecraft:iron_ore"));
        assertTrue(settings.secondSmeltEnabledForOutput("minecraft:iron_ingot"));
        assertFalse(settings.secondSmeltEnabledForOutput("minecraft:glass"));
        assertEquals(5, settings.vanillaXpMultiplier(1000));
    }
}

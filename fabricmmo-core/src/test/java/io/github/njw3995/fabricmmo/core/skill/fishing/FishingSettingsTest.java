package io.github.njw3995.fabricmmo.core.skill.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FishingSettingsTest {
    @Test
    void loadsPinnedUpstreamRetroDefaults() throws Exception {
        FishingSettings settings = settings();

        assertEquals(ProgressionMode.RETRO, settings.progressionMode());
        assertTrue(settings.dropsEnabled());
        assertTrue(settings.overrideVanillaTreasures());
        assertFalse(settings.extraFish());
        assertEquals(4.0D, settings.lureModifierPercent());
        assertFalse(settings.allowConflictingEnchants());
        assertTrue(settings.exploitFixEnabled());
        assertEquals(3, settings.exploitMoveRange());
        assertEquals(10, settings.overFishLimit());

        assertEquals(1, settings.treasureHunterRank(1));
        assertEquals(2, settings.treasureHunterRank(250));
        assertEquals(8, settings.treasureHunterRank(1000));
        assertEquals(1, settings.masterAnglerRank(1));
        assertEquals(8, settings.masterAnglerRank(900));
        assertEquals(0, settings.shakeRank(149));
        assertEquals(1, settings.shakeRank(150));
        assertEquals(5, settings.fishermansDietRank(1000));
        assertEquals(200, settings.magicHunterUnlockLevel());
        assertEquals(50, settings.iceFishingUnlockLevel());
        assertEquals(75.0D, settings.shakeChance(700, false));
        assertEquals(99.975D, settings.shakeChance(700, true));
        assertEquals(5, settings.vanillaXpMultiplier(1000));
        assertEquals(50, settings.shakeXp());
    }

    @Test
    void masterAnglerUsesUpstreamReductionsCapsBoatAndLure() throws Exception {
        FishingSettings settings = settings();

        assertEquals(new FishingSettings.WaitBounds(90, 570, 10, 30),
                settings.masterAnglerBounds(1, false, 0));
        assertEquals(new FishingSettings.WaitBounds(40, 230, 90, 370),
                settings.masterAnglerBounds(900, true, 100));
    }

    private static FishingSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return FishingSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("experience.yml"));
    }
}

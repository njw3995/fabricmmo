package io.github.njw3995.fabricmmo.core.skill.herbalism;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HerbalismSettingsTest {
    @Test
    void packagedDefaultsMatchPinnedUpstreamRetroMode() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        HerbalismSettings settings = HerbalismSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("experience.yml"));
        assertEquals(240, settings.greenTerraCooldownSeconds());
        assertEquals(50, settings.greenTerraUnlockLevel());
        assertEquals(3, settings.greenTerraDurationSeconds(50));
        assertEquals(0, settings.greenThumbRank(249));
        assertEquals(1, settings.greenThumbRank(250));
        assertEquals(5, settings.farmersDietRank(1000));
        assertEquals(100.0D, settings.doubleDropsChance(1000, false));
        assertEquals(5.0D, settings.hylianLuckChance(500, false));
        assertTrue(settings.replantEnabled("wheat"));
        assertTrue(settings.preventAfkLeveling());
        assertTrue(settings.limitXpOnTallPlants());
    }
}

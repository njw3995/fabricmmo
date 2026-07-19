package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WoodcuttingSettingsTest {
    @Test
    void packagedDefaultsMatchPinnedUpstreamAndMinecraft1211() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        WoodcuttingSettings settings = WoodcuttingSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("experience.yml"));
        WoodcuttingDropSettings drops = WoodcuttingDropSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"));
        assertEquals(240, settings.treeFellerCooldownSeconds());
        assertEquals(50, settings.treeFellerUnlockLevel());
        assertEquals(150, settings.leafBlowerUnlockLevel());
        assertEquals(1000, settings.treeFellerThreshold());
        assertTrue(settings.treeFellerReducedXp());
        assertEquals(50.0D, drops.cleanCutsChanceMaxPercent());
        assertEquals(100.0D, drops.harvestLumberChanceMaxPercent());
    }
}

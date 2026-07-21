package io.github.njw3995.fabricmmo.core.skill.maces;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MacesSettingsTest {
    @Test
    void loadsPinnedUpstreamDefaultsFromBundledConfigs() throws Exception {
        MacesSettings settings = settings();

        assertEquals(ProgressionMode.RETRO, settings.progressionMode());
        assertTrue(settings.pvpEnabled());
        assertTrue(settings.pveEnabled());
        assertTrue(settings.adjustForAttackCooldown());
        assertFalse(settings.limitBreakPve());
        assertTrue(settings.crippleEffectEnabled());
        assertEquals("minecraft:item.mace.smash_ground", settings.crippleSound().id());
        assertEquals(1.0D, settings.crippleSound().volume(), 1.0E-9);
        assertEquals(0.5D, settings.crippleSound().pitch(), 1.0E-9);

        assertEquals(10, settings.limitBreakRank(1000));
        assertEquals(4, settings.crippleRank(800));
        assertEquals(4, settings.crushRank(900));
        assertEquals(33.0D, settings.crippleChancePercent(800, false), 1.0E-9);
        assertEquals(4.5D, settings.crushDamage(900), 1.0E-9);
        assertEquals(20, MacesSettings.crippleDurationTicks(true));
        assertEquals(30, MacesSettings.crippleDurationTicks(false));
        assertEquals(1, MacesSettings.crippleAmplifier(true));
        assertEquals(2, MacesSettings.crippleAmplifier(false));
    }

    @Test
    void arrayAccessorsCannotMutateLoadedSettings() throws Exception {
        MacesSettings settings = settings();
        int[] unlocks = settings.crippleUnlocksRetro();
        unlocks[0] = 999;
        double[] chances = settings.crippleChances();
        chances[3] = 999.0D;

        assertArrayEquals(new int[] {50, 200, 400, 800}, settings.crippleUnlocksRetro());
        assertArrayEquals(new double[] {10.0D, 15.0D, 20.0D, 33.0D},
                settings.crippleChances(), 1.0E-9);
    }

    private static MacesSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return MacesSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("sounds.yml"));
    }
}

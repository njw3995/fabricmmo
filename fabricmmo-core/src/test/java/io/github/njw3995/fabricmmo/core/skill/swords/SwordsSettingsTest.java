package io.github.njw3995.fabricmmo.core.skill.swords;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SwordsSettingsTest {
    @Test
    void loadsPinnedUpstreamDefaultsFromBundledConfigs() throws Exception {
        SwordsSettings settings = settings();

        assertEquals(ProgressionMode.RETRO, settings.progressionMode());
        assertTrue(settings.pvpEnabled());
        assertTrue(settings.pveEnabled());
        assertTrue(settings.adjustForAttackCooldown());
        assertFalse(settings.limitBreakPve());
        assertTrue(settings.toolReadyNotification().actionBar());
        assertFalse(settings.toolReadyNotification().copyToChat());
        assertTrue(settings.abilityCooldownNotification().actionBar());
        assertTrue(settings.superAbilityAlertOthersNotification().copyToChat());
        assertEquals("minecraft:item.armor.equip_gold", settings.toolReadySound().id());
        assertEquals(1.0D, settings.toolReadySound().volume(), 1.0E-9);
        assertEquals(0.4D, settings.toolReadySound().pitch(), 1.0E-9);
        assertEquals("minecraft:item.trident.riptide_3", settings.abilityActivatedSound().id());
        assertEquals(0.1D, settings.abilityActivatedSound().pitch(), 1.0E-9);
        assertEquals(240, settings.serratedCooldownSeconds());
        assertEquals(50, settings.serratedUnlockLevel());
        assertEquals(3, settings.serratedDurationSeconds(50));
        assertEquals(22, settings.serratedDurationSeconds(1000));

        assertArrayEquals(new int[] {1, 150, 750, 900}, settings.ruptureUnlocks());
        assertEquals(4, settings.ruptureRank(900));
        assertEquals(66.0D, settings.ruptureChancePercent(4, false), 1.0E-9);
        assertEquals(0.3D, settings.ruptureTickDamage(4, true), 1.0E-9);
        assertEquals(1.0D, settings.ruptureTickDamage(4, false), 1.0E-9);
        assertEquals(5, settings.ruptureDurationSeconds(true));
        assertEquals(5, settings.ruptureDurationSeconds(false));

        assertEquals(1, settings.stabRank(750));
        assertEquals(2, settings.stabRank(1000));
        assertEquals(2.5D, settings.stabDamage(750), 1.0E-9);
        assertEquals(4.0D, settings.stabDamage(1000), 1.0E-9);
        assertEquals(10, settings.limitBreakRank(1000));
        assertEquals(30.0D, settings.counterChancePercent(1000, false), 1.0E-9);
    }

    @Test
    void arrayAccessorsCannotMutateLoadedSettings() throws Exception {
        SwordsSettings settings = settings();
        int[] unlocks = settings.ruptureUnlocksRetro();
        double[] chances = settings.ruptureChance();
        unlocks[0] = 999;
        chances[0] = 999.0D;

        assertEquals(1, settings.ruptureUnlocksRetro()[0]);
        assertEquals(15.0D, settings.ruptureChance()[0], 1.0E-9);
    }

    private static SwordsSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return SwordsSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("sounds.yml"));
    }
}

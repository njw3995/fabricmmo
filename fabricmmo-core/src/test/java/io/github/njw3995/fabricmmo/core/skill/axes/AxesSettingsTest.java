package io.github.njw3995.fabricmmo.core.skill.axes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AxesSettingsTest {
    @Test
    void loadsPinnedUpstreamDefaultsFromBundledConfigs() throws Exception {
        AxesSettings settings = settings();

        assertEquals(ProgressionMode.RETRO, settings.progressionMode());
        assertTrue(settings.pvpEnabled());
        assertTrue(settings.pveEnabled());
        assertTrue(settings.adjustForAttackCooldown());
        assertFalse(settings.limitBreakPve());
        assertTrue(settings.greaterImpactParticles());
        assertEquals("minecraft:item.armor.equip_gold", settings.toolReadySound().id());
        assertEquals("minecraft:item.trident.riptide_3", settings.abilityActivatedSound().id());
        assertEquals(240, settings.skullSplitterCooldownSeconds());
        assertEquals(50, settings.skullSplitterUnlockLevel());
        assertEquals(3, settings.skullSplitterDurationSeconds(50));
        assertEquals(22, settings.skullSplitterDurationSeconds(1000));

        assertEquals(4, settings.axeMasteryRank(200));
        assertEquals(4.0D, settings.axeMasteryDamage(200), 1.0E-9);
        assertEquals(20, settings.armorImpactRank(1000));
        assertEquals(130.0D, settings.armorImpactRawDamage(1000), 1.0E-9);
        assertEquals(37.5D, settings.criticalChancePercent(1000, false), 1.0E-9);
        assertEquals(1, settings.greaterImpactRank(250));
        assertEquals(10, settings.limitBreakRank(1000));
        assertEquals(25.0D, settings.greaterImpactChance(), 1.0E-9);
        assertEquals(25.0D, settings.armorImpactChance(), 1.0E-9);
        assertEquals(2.0D, settings.skullSplitterDamageModifier(), 1.0E-9);
    }

    @Test
    void arrayAccessorsCannotMutateLoadedSettings() throws Exception {
        AxesSettings settings = settings();
        int[] unlocks = settings.limitBreakUnlocksRetro();
        unlocks[0] = 999;

        assertArrayEquals(new int[] {100, 200, 300, 400, 500, 600, 700, 800, 900, 1000},
                settings.limitBreakUnlocksRetro());
    }

    private static AxesSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return AxesSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("sounds.yml"));
    }
}

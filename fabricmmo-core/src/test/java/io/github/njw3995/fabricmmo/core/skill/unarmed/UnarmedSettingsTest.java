package io.github.njw3995.fabricmmo.core.skill.unarmed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class UnarmedSettingsTest {
    @Test
    void loadsPinnedUpstreamDefaults() throws Exception {
        UnarmedSettings settings = settings();

        assertEquals(ProgressionMode.RETRO, settings.progressionMode());
        assertTrue(settings.pvpEnabled());
        assertTrue(settings.pveEnabled());
        assertTrue(settings.adjustForAttackCooldown());
        assertFalse(settings.limitBreakPve());
        assertFalse(settings.itemsAsUnarmed());
        assertTrue(settings.blockCrackerAllowed());
        assertFalse(settings.disarmAntiTheft());
        assertEquals(240, settings.berserkCooldownSeconds());
        assertEquals("minecraft:block.glass.break", settings.glassSound().id());
        assertEquals(1.0D, settings.glassSound().volume(), 1.0E-9);
        assertEquals("minecraft:entity.item.pickup", settings.popSound().id());
        assertEquals(0.2D, settings.popSound().volume(), 1.0E-9);
        assertEquals(50, settings.berserkUnlockLevel());
        assertEquals(3, settings.berserkDurationSeconds(50));
        assertEquals(22, settings.berserkDurationSeconds(1000));
        assertEquals(33.0D, settings.disarmChancePercent(1000, false), 1.0E-9);
        assertEquals(50.0D, settings.arrowDeflectChancePercent(1000, false), 1.0E-9);
        assertEquals(100.0D, settings.ironGripChancePercent(1000, false), 1.0E-9);
        assertEquals(13.5D, settings.steelArmDamage(1000), 1.0E-9);
        assertEquals(10, settings.limitBreakRank(1000));
    }

    @Test
    void arrayAccessorsCannotMutateSettings() throws Exception {
        UnarmedSettings settings = settings();
        int[] unlocks = settings.limitBreakUnlocksRetro();
        unlocks[0] = 999;
        assertArrayEquals(new int[] {100, 200, 300, 400, 500, 600, 700, 800, 900, 1000},
                settings.limitBreakUnlocksRetro());
    }

    private static UnarmedSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return UnarmedSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("sounds.yml"));
    }
}

package io.github.njw3995.fabricmmo.core.skill.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CombatXpSettingsTest {
    @Test
    void loadsPinnedCombatMultipliersAndDamageCeiling() throws Exception {
        CombatXpSettings settings = CombatXpSettings.load(
                Path.of("src/main/resources/defaults/experience.yml"));

        assertTrue(settings.pvpRewards());
        assertEquals(20.0D, settings.pvpXp(), 1.0E-9);
        assertEquals(40.0D, settings.pveXp("creeper", false, true), 1.0E-9);
        assertEquals(10.0D, settings.pveXp("cow", true, false), 1.0E-9);
        assertEquals(12.0D, settings.pveXp("mooshroom", true, false), 1.0E-9);
        assertEquals(0.0D, settings.pveXp("snow_golem", false, false), 1.0E-9);
        assertEquals(0.0D, settings.pveXp("unknown_hostile", false, true), 1.0E-9);
        assertEquals(10.0D, settings.pveXp("unknown_passive", false, false), 1.0E-9);
        assertEquals(0.0D, settings.originMultiplier(CombatXpSettings.Origin.EGG), 1.0E-9);
        assertEquals(0.0D, settings.originMultiplier(CombatXpSettings.Origin.SPAWNER), 1.0E-9);
        assertEquals(1.0D, settings.originMultiplier(CombatXpSettings.Origin.BRED), 1.0E-9);
        assertEquals(0.0D,
                settings.originMultiplier(CombatXpSettings.Origin.PLAYER_TAMED), 1.0E-9);
        assertEquals(100.0D, settings.cappedDamage(150.0D), 1.0E-9);
        assertEquals(25.0D, settings.cappedDamage(25.0D), 1.0E-9);
        assertEquals(119, settings.awardXp(40.0D, 2.999D));
        assertEquals(4000, settings.awardXp(40.0D, 150.0D));
    }

    @Test
    void entityNamesNormalizeLikeUpstreamConfigurationKeys() {
        assertEquals("zombified_piglin", CombatXpSettings.normalize("Zombified Piglin"));
        assertEquals("cave_spider", CombatXpSettings.normalize("Cave-Spider"));
        assertEquals("mushroom_cow", CombatXpSettings.upstreamEntityKey("mooshroom"));
        assertEquals("snowman", CombatXpSettings.upstreamEntityKey("snow_golem"));
    }
}

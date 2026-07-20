package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AcrobaticsSettingsTest {
    @Test
    void loadsPinnedUpstreamDefaultsFromBundledConfigs() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        AcrobaticsSettings settings = AcrobaticsSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("experience.yml"),
                defaults.resolve("sounds.yml"));

        assertTrue(settings.pveEnabled());
        assertTrue(settings.pvpEnabled());
        assertEquals(20.0D, settings.dodgeChanceMax(), 1.0E-9);
        assertEquals(2.0D, settings.dodgeDamageModifier(), 1.0E-9);
        assertEquals(100.0D, settings.rollChanceMax(), 1.0E-9);
        assertEquals(14.0D, settings.effectiveSuccessfulRollDamageThreshold(), 1.0E-9);
        assertEquals(800.0D, settings.dodgeXpModifier(), 1.0E-9);
        assertEquals(600.0D, settings.rollXpModifier(), 1.0E-9);
        assertEquals(600.0D, settings.fallXpModifier(), 1.0E-9);
        assertEquals(2.0D, settings.featherFallingXpMultiplier(), 1.0E-9);
        assertTrue(settings.subSkillMessageActionBar());
        assertFalse(settings.subSkillMessageCopyToChat());
        assertTrue(settings.rollSoundEnabled());
        assertEquals("minecraft:entity.llama.swag", settings.rollSoundId());
        assertEquals(1.0D, settings.rollSoundVolume(), 1.0E-9);
        assertEquals(1.2D, settings.rollSoundPitch(), 1.0E-9);
        assertTrue(settings.pinnedDodgeCombatEnabled());
    }
}

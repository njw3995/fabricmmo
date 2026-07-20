package io.github.njw3995.fabricmmo.core.skill.swords;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import org.junit.jupiter.api.Test;

class SwordsProbabilityTest {
    @Test
    void counterScalesToTheConfiguredCapForEachProgressionMode() {
        assertEquals(15.0D, SwordsProbability.counterChancePercent(
                500, ProgressionMode.RETRO, 30.0D, 100, 1000, false), 1.0E-9);
        assertEquals(30.0D, SwordsProbability.counterChancePercent(
                1000, ProgressionMode.RETRO, 30.0D, 100, 1000, false), 1.0E-9);
        assertEquals(15.0D, SwordsProbability.counterChancePercent(
                50, ProgressionMode.STANDARD, 30.0D, 100, 1000, false), 1.0E-9);
    }

    @Test
    void luckyUsesThePinnedStaticSkillMultiplier() {
        assertEquals(39.99D, SwordsProbability.counterChancePercent(
                1000, ProgressionMode.RETRO, 30.0D, 100, 1000, true), 1.0E-9);
        assertEquals(87.978D,
                SwordsProbability.ruptureChancePercent(66.0D, true), 1.0E-9);
    }
}

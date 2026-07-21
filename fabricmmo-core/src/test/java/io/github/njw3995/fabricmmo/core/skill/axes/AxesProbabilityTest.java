package io.github.njw3995.fabricmmo.core.skill.axes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import org.junit.jupiter.api.Test;

class AxesProbabilityTest {
    @Test
    void criticalScalesToTheConfiguredCapForEachProgressionMode() {
        assertEquals(18.75D, AxesProbability.criticalChancePercent(
                500, ProgressionMode.RETRO, 37.5D, 100, 1000, false), 1.0E-9);
        assertEquals(37.5D, AxesProbability.criticalChancePercent(
                1000, ProgressionMode.RETRO, 37.5D, 100, 1000, false), 1.0E-9);
        assertEquals(18.75D, AxesProbability.criticalChancePercent(
                50, ProgressionMode.STANDARD, 37.5D, 100, 1000, false), 1.0E-9);
    }

    @Test
    void luckyUsesThePinnedStaticSkillMultiplier() {
        assertEquals(49.9875D, AxesProbability.criticalChancePercent(
                1000, ProgressionMode.RETRO, 37.5D, 100, 1000, true), 1.0E-9);
        assertEquals(33.325D, AxesProbability.staticChancePercent(25.0D, true), 1.0E-9);
    }

    @Test
    void attackStrengthScalesEveryChanceRoll() {
        assertTrue(AxesProbability.succeeds(12.4D, 25.0D, 0.5D));
        assertFalse(AxesProbability.succeeds(12.5D, 25.0D, 0.5D));
        assertTrue(AxesProbability.succeeds(24.9D, 25.0D, 1.0D));
    }
}

package io.github.njw3995.fabricmmo.core.skill.unarmed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import org.junit.jupiter.api.Test;

class UnarmedProbabilityTest {
    @Test
    void chanceCurvesReachTheirConfiguredCaps() {
        assertEquals(16.5D, UnarmedProbability.chancePercent(
                500, ProgressionMode.RETRO, 33.0D, 100, 1000, false), 1.0E-9);
        assertEquals(33.0D, UnarmedProbability.chancePercent(
                1000, ProgressionMode.RETRO, 33.0D, 100, 1000, false), 1.0E-9);
        assertEquals(25.0D, UnarmedProbability.chancePercent(
                50, ProgressionMode.STANDARD, 50.0D, 100, 1000, false), 1.0E-9);
    }

    @Test
    void luckyUsesThePinnedStaticMultiplier() {
        assertEquals(43.989D, UnarmedProbability.chancePercent(
                1000, ProgressionMode.RETRO, 33.0D, 100, 1000, true), 1.0E-9);
    }

    @Test
    void onlyDisarmUsesCommittedAttackStrength() {
        assertTrue(UnarmedProbability.succeeds(12.4D, 25.0D, 0.5D));
        assertFalse(UnarmedProbability.succeeds(12.5D, 25.0D, 0.5D));
        assertTrue(UnarmedProbability.succeedsUnscaled(24.9D, 25.0D));
        assertFalse(UnarmedProbability.succeedsUnscaled(25.0D, 25.0D));
    }
}

package io.github.njw3995.fabricmmo.core.skill.ranged;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RangedProbabilityTest {
    @Test
    void scalesLinearlyToTheConfiguredCap() {
        assertEquals(0.0D, RangedProbability.chancePercent(0, 1000, 50.0D, false), 1.0E-9);
        assertEquals(25.0D, RangedProbability.chancePercent(500, 1000, 50.0D, false), 1.0E-9);
        assertEquals(50.0D, RangedProbability.chancePercent(2000, 1000, 50.0D, false), 1.0E-9);
    }

    @Test
    void appliesThePinnedLuckyMultiplierAfterNormalChanceCalculation() {
        assertEquals(66.65D,
                RangedProbability.chancePercent(1000, 1000, 50.0D, true), 1.0E-9);
        assertEquals(133.3D,
                RangedProbability.chancePercent(1000, 1000, 100.0D, true), 1.0E-9);
    }
}

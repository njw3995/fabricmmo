package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import org.junit.jupiter.api.Test;

class AcrobaticsProbabilityTest {
    @Test
    void retroCurvesMatchPinnedUpstreamValues() {
        assertEquals(10.0D, AcrobaticsProbability.chancePercent(
                500, ProgressionMode.RETRO, 20.0D, 100, 1000, false), 1.0E-9);
        assertEquals(50.0D, AcrobaticsProbability.chancePercent(
                500, ProgressionMode.RETRO, 100.0D, 100, 1000, false), 1.0E-9);
        assertEquals(100.0D, AcrobaticsProbability.chancePercent(
                1000, ProgressionMode.RETRO, 100.0D, 100, 1000, false), 1.0E-9);
    }

    @Test
    void gracefulRollIsExactlyDoubleNormalRoll() {
        assertEquals(100.0D, AcrobaticsProbability.gracefulRollChancePercent(50.0D), 1.0E-9);
        assertEquals(200.0D, AcrobaticsProbability.gracefulRollChancePercent(100.0D), 1.0E-9);
    }

    @Test
    void luckyUsesUpstreamOnePointThreeThreeThreeMultiplier() {
        assertEquals(13.33D, AcrobaticsProbability.chancePercent(
                500, ProgressionMode.RETRO, 20.0D, 100, 1000, true), 1.0E-9);
    }
}

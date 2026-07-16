package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MiningProbabilityTest {
    @Test
    void scalesLinearlyToConfiguredCeilingLikeUpstreamProbabilityUtil() {
        assertEquals(0.0D, MiningProbability.chance(0, 1000, 100.0D, false));
        assertEquals(0.5D, MiningProbability.chance(500, 1000, 100.0D, false));
        assertEquals(1.0D, MiningProbability.chance(1000, 1000, 100.0D, false));
        assertEquals(1.0D, MiningProbability.chance(1500, 1000, 100.0D, false));
    }

    @Test
    void appliesUpstreamLuckyMultiplier() {
        assertEquals(0.6665D,
                MiningProbability.chance(500, 1000, 100.0D, true),
                0.0000001D);
    }
}

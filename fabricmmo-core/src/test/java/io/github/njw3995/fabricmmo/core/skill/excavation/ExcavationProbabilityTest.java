package io.github.njw3995.fabricmmo.core.skill.excavation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExcavationProbabilityTest {
    @Test
    void luckyUsesUpstreamOnePointThreeThreeThreeMultiplier() {
        assertFalse(ExcavationProbability.succeeds(0.12D, 10.0D, false));
        assertTrue(ExcavationProbability.succeeds(0.12D, 10.0D, true));
    }


    @Test
    void exactProbabilityBoundarySucceedsLikeUpstream() {
        assertTrue(ExcavationProbability.succeeds(0.10D, 10.0D, false));
    }

    @Test
    void chanceIsClampedAtOneHundredPercent() {
        assertTrue(ExcavationProbability.succeeds(0.999D, 100.0D, true));
    }
}

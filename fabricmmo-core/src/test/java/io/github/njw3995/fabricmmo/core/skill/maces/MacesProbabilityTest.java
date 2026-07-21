package io.github.njw3995.fabricmmo.core.skill.maces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MacesProbabilityTest {
    @Test
    void luckyUsesPinnedStaticSkillMultiplier() {
        assertEquals(43.989D, MacesProbability.chancePercent(33.0D, true), 1.0E-9);
    }

    @Test
    void attackStrengthScalesCrippleChance() {
        assertTrue(MacesProbability.succeeds(16.4D, 33.0D, 0.5D));
        assertFalse(MacesProbability.succeeds(16.5D, 33.0D, 0.5D));
        assertTrue(MacesProbability.succeeds(32.9D, 33.0D, 1.0D));
    }
}

package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BlastMiningMathTest {
    @Test
    void usesPinnedUpstreamRankDefaults() {
        assertEquals(4, BlastMiningDefaults.rankForLevel(50, false));
        assertEquals(4, BlastMiningDefaults.rankForLevel(500, true));
        assertEquals(50, BlastMiningDefaults.firstDamageReductionUnlock(false));
        assertEquals(500, BlastMiningDefaults.firstDamageReductionUnlock(true));
        assertEquals(10, BlastMiningDefaults.firstRadiusIncreaseUnlock(false));
        assertEquals(100, BlastMiningDefaults.firstRadiusIncreaseUnlock(true));
        assertEquals(3, BlastMiningDefaults.dropMultiplier(8));
    }

    @Test
    void appliesRadiusDamageAndPvpCapFormulas() {
        assertEquals(6.0F, BlastMiningMath.radius(4.0F, 3));
        assertEquals(75.0D, BlastMiningMath.ownerDamage(100.0D, 4));
        assertEquals(0.0D, BlastMiningMath.ownerDamage(100.0D, 8));
        assertEquals(24.0D, BlastMiningMath.bystanderDamage(60.0D));
        assertEquals(0.0D, BlastMiningMath.bystanderDamage(-3.0D));
    }

    @Test
    void capsOreYieldAtThreeLikeUpstreamBlastMining() {
        assertEquals(1.35F, BlastMiningMath.oreYield(1.0F, 1), 0.00001F);
        assertEquals(3.0F, BlastMiningMath.oreYield(2.0F, 8));
    }
}

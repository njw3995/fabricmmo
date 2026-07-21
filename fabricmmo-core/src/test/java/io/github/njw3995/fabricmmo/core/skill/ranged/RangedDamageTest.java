package io.github.njw3995.fabricmmo.core.skill.ranged;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RangedDamageTest {
    @Test
    void skillAndPoweredShotUseNonAdditiveRankedPercentageWithBonusCap() {
        assertEquals(10.0D, RangedDamage.rankedPercentBonus(10.0D, 0, 10.0D, 9.0D));
        assertEquals(11.0D, RangedDamage.rankedPercentBonus(10.0D, 1, 10.0D, 9.0D));
        assertEquals(19.0D, RangedDamage.rankedPercentBonus(10.0D, 20, 10.0D, 9.0D));
    }

    @Test
    void limitBreakUsesPinnedArmorBands() {
        assertEquals(2, RangedDamage.limitBreakDamage(10, 4));
        assertEquals(5, RangedDamage.limitBreakDamage(10, 8));
        assertEquals(7, RangedDamage.limitBreakDamage(10, 12));
        assertEquals(10, RangedDamage.limitBreakDamage(10, 13));
    }

    @Test
    void impaleUsesCurrentUpstreamRankFormula() {
        assertEquals(0.0D, RangedDamage.impaleDamage(0, 1.0D, 0.5D), 1.0E-9);
        assertEquals(1.5D, RangedDamage.impaleDamage(1, 1.0D, 0.5D), 1.0E-9);
        assertEquals(6.0D, RangedDamage.impaleDamage(10, 1.0D, 0.5D), 1.0E-9);
    }
}

package io.github.njw3995.fabricmmo.core.skill.unarmed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UnarmedDamageTest {
    @Test
    void steelArmUsesThePinnedTwentyRankFormula() {
        double[] overrides = new double[20];
        assertEquals(1.0D, UnarmedDamage.steelArmDamage(1, false, overrides), 1.0E-9);
        assertEquals(9.0D, UnarmedDamage.steelArmDamage(17, false, overrides), 1.0E-9);
        assertEquals(10.5D, UnarmedDamage.steelArmDamage(18, false, overrides), 1.0E-9);
        assertEquals(12.0D, UnarmedDamage.steelArmDamage(19, false, overrides), 1.0E-9);
        assertEquals(13.5D, UnarmedDamage.steelArmDamage(20, false, overrides), 1.0E-9);
    }

    @Test
    void steelArmOverrideUsesTheConfiguredRankValue() {
        double[] overrides = new double[20];
        overrides[3] = 7.25D;
        assertEquals(7.25D, UnarmedDamage.steelArmDamage(4, true, overrides), 1.0E-9);
    }

    @Test
    void berserkPreservesThePinnedDoubleAttackStrengthApplication() {
        assertEquals(5.0D, UnarmedDamage.berserkBonus(10.0D, 1.0D), 1.0E-9);
        assertEquals(15.0D, UnarmedDamage.finalBerserkDamage(10.0D, 1.0D), 1.0E-9);
        assertEquals(-2.5D, UnarmedDamage.berserkBonus(10.0D, 0.5D), 1.0E-9);
        assertEquals(8.75D, UnarmedDamage.finalBerserkDamage(10.0D, 0.5D), 1.0E-9);
        assertEquals(10.0D, UnarmedDamage.finalBerserkDamage(10.0D, 0.0D), 1.0E-9);
    }

    @Test
    void limitBreakUsesTheUpstreamArmorQualityBands() {
        assertEquals(2, UnarmedDamage.limitBreakDamage(10, 4));
        assertEquals(5, UnarmedDamage.limitBreakDamage(10, 8));
        assertEquals(7, UnarmedDamage.limitBreakDamage(10, 12));
        assertEquals(10, UnarmedDamage.limitBreakDamage(10, 13));
    }
}

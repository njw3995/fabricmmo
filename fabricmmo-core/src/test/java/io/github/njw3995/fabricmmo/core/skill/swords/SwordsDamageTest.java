package io.github.njw3995.fabricmmo.core.skill.swords;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SwordsDamageTest {
    @Test
    void stabUsesPinnedBaseAndPerRankFormula() {
        assertEquals(0.0D, SwordsDamage.stabDamage(0, 1.0D, 1.5D), 1.0E-9);
        assertEquals(2.5D, SwordsDamage.stabDamage(1, 1.0D, 1.5D), 1.0E-9);
        assertEquals(4.0D, SwordsDamage.stabDamage(2, 1.0D, 1.5D), 1.0E-9);
    }

    @Test
    void counterReflectsConfiguredFractionOfIncomingRawDamage() {
        assertEquals(5.0D, SwordsDamage.counterDamage(10.0D, 2.0D), 1.0E-9);
        assertEquals(0.0D, SwordsDamage.counterDamage(-1.0D, 2.0D), 1.0E-9);
    }

    @Test
    void serratedAoeHasOneDamageMinimum() {
        assertEquals(2.0D, SwordsDamage.serratedAoeDamage(8.0D, 4.0D), 1.0E-9);
        assertEquals(1.0D, SwordsDamage.serratedAoeDamage(2.0D, 4.0D), 1.0E-9);
    }

    @Test
    void limitBreakUsesUpstreamArmorQualityBandsAndIntegerTruncation() {
        assertEquals(2, SwordsDamage.limitBreakDamage(10, 4));
        assertEquals(5, SwordsDamage.limitBreakDamage(10, 8));
        assertEquals(7, SwordsDamage.limitBreakDamage(10, 12));
        assertEquals(10, SwordsDamage.limitBreakDamage(10, 13));
        assertEquals(10, SwordsDamage.limitBreakDamage(10, 1000));
    }

    @Test
    void attackStrengthUsesRawDamageOverAttackAttributeAndClamps() {
        assertEquals(0.5D, SwordsDamage.attackStrengthScale(5.0D, 10.0D), 1.0E-9);
        assertEquals(1.0D, SwordsDamage.attackStrengthScale(15.0D, 10.0D), 1.0E-9);
        assertEquals(0.0D, SwordsDamage.attackStrengthScale(-1.0D, 10.0D), 1.0E-9);
        assertEquals(1.0D, SwordsDamage.attackStrengthScale(5.0D, 0.0D), 1.0E-9);
    }
}

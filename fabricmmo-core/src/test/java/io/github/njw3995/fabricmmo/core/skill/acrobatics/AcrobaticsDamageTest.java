package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AcrobaticsDamageTest {
    @Test
    void dodgeDividesDamageAndNeverDropsBelowOne() {
        assertEquals(5.0D, AcrobaticsDamage.dodgeDamage(10.0D, 2.0D), 1.0E-9);
        assertEquals(1.0D, AcrobaticsDamage.dodgeDamage(1.0D, 2.0D), 1.0E-9);
    }

    @Test
    void rollSubtractsThresholdAndClampsAtZero() {
        assertEquals(4.0D, AcrobaticsDamage.rollDamage(10.0D, 6.0D), 1.0E-9);
        assertEquals(0.0D, AcrobaticsDamage.rollDamage(10.0D, 14.0D), 1.0E-9);
    }

    @Test
    void fallXpClampsDamageAtTwentyAndDoublesForFeatherFalling() {
        assertEquals(12_000.0D,
                AcrobaticsDamage.fallXp(30.0D, false, 600.0D, 600.0D, false, 2.0D),
                1.0E-9);
        assertEquals(24_000.0D,
                AcrobaticsDamage.fallXp(30.0D, true, 600.0D, 600.0D, true, 2.0D),
                1.0E-9);
    }

    @Test
    void rollResultsTruncateFloatXpRatherThanRounding() {
        assertEquals(123, AcrobaticsDamage.rollResultXp(123.999D));
    }

    @Test
    void fallXpUsesUpstreamFloatPrecision() {
        double expected = (double) (float) (3.333333D * 1.234567D);
        assertEquals(expected,
                AcrobaticsDamage.fallXp(
                        3.333333D, false, 9.0D, 1.234567D, false, 2.0D),
                0.0D);
    }

    @Test
    void fatalUsesTheSameLessThanOrEqualGateAsUpstream() {
        assertTrue(AcrobaticsDamage.fatal(5.0D, 5.0D));
        assertFalse(AcrobaticsDamage.fatal(5.0D, 4.999D));
    }
}

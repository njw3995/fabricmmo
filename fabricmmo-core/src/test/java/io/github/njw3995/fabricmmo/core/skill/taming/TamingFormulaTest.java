package io.github.njw3995.fabricmmo.core.skill.taming;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TamingFormulaTest {
    @Test
    void matchesPinnedDamageDefaults() {
        assertEquals(12.0F, TamingFormula.goreDamage(6.0F, 2.0D));
        assertEquals(8.0F, TamingFormula.sharpenedClaws(6.0F, 2.0D));
        assertEquals(6.0F, TamingFormula.reducedDamage(12.0F, 2.0D));
        assertEquals(2.0F, TamingFormula.reducedDamage(12.0F, 6.0D));
    }

    @Test
    void appliesLuckyChanceAndClampsIt() {
        assertEquals(66.66666666666666D, TamingFormula.catalyzedChance(50.0D, true), 1.0E-12D);
        assertEquals(50.0D, TamingFormula.catalyzedChance(50.0D, false));
        assertEquals(100.0D, TamingFormula.catalyzedChance(90.0D, true));
        assertEquals(0.0D, TamingFormula.catalyzedChance(-1.0D, false));
    }

    @Test
    void invalidReductionDivisorLeavesDamageUnchanged() {
        assertEquals(7.0F, TamingFormula.reducedDamage(7.0F, 0.0D));
        assertEquals(7.0F, TamingFormula.reducedDamage(7.0F, -1.0D));
    }
}

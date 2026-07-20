package io.github.njw3995.fabricmmo.core.skill.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class RepairFormulaTest {
    @Test
    void durabilityUsesUpstreamIntegerTruncation() {
        assertEquals(76, RepairFormula.repairedDamage(
                200, 250, 4, 500, true, 200.0D, 1000, false));
    }

    @Test
    void superRepairDoublesAfterMasteryAndClampsAtZero() {
        assertEquals(52, RepairFormula.repairedDamage(
                300, 250, 4, 500, true, 200.0D, 1000, true));
        assertEquals(0, RepairFormula.repairedDamage(
                20, 250, 4, 1000, true, 200.0D, 1000, true));
    }

    @Test
    void zeroBaseRepairAmountUsesShortMaximumFallback() {
        assertEquals(0, RepairFormula.repairedDamage(
                10, 3, 4, 0, false, 200.0D, 1000, false));
    }

    @Test
    void xpUsesActualDurabilityRestored() {
        assertEquals(1250.0D, RepairFormula.xp(
                100, 50, 200, 2.0D, 1000.0D, 2.5D), 0.00001D);
    }
}

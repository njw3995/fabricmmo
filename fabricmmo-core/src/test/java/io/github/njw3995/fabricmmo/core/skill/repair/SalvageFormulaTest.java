package io.github.njw3995.fabricmmo.core.skill.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class SalvageFormulaTest {
    @Test
    void damagedRecoveryUsesDeterministicFloor() {
        assertEquals(3, SalvageFormula.recoverableAmount(31, 100, 5));
        assertEquals(0, SalvageFormula.recoverableAmount(99, 100, 5));
    }

    @Test
    void scrapCollectorCapMatchesRankOneSpecialCase() {
        assertEquals(1, SalvageFormula.scrapCollectorLimit(1));
        assertEquals(8, SalvageFormula.scrapCollectorLimit(4));
        assertEquals(4, SalvageFormula.recoveredAmount(20, 100, 8, 2));
    }
}

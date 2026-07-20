package io.github.njw3995.fabricmmo.core.skill.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ArcaneSalvageFormulaTest {
    @Test
    void fullPartialAndLostOutcomesMatchUpstreamOrder() {
        assertEquals(ArcaneSalvageFormula.Outcome.FULL,
                ArcaneSalvageFormula.resolve(4, 5, false, true,
                        true, false, true, false).outcome());
        assertEquals(new ArcaneSalvageFormula.Result(
                ArcaneSalvageFormula.Outcome.PARTIAL, 3),
                ArcaneSalvageFormula.resolve(4, 5, false, true,
                        true, false, false, true));
        assertEquals(ArcaneSalvageFormula.Outcome.LOST,
                ArcaneSalvageFormula.resolve(4, 5, false, true,
                        true, false, false, false).outcome());
    }

    @Test
    void unsafeAndBypassPreserveFullLevel() {
        assertEquals(8, ArcaneSalvageFormula.resolve(8, 5, true,
                true, true, false, true, false).level());
        assertEquals(5, ArcaneSalvageFormula.resolve(8, 5, false,
                true, true, true, false, false).level());
    }
}

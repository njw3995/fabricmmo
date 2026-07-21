package io.github.njw3995.fabricmmo.core.skill.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ArcaneForgingFormulaTest {
    @Test
    void noRankLosesEnchantWhenLossEnabled() {
        assertEquals(new ArcaneForgingFormula.Result(
                ArcaneForgingFormula.Outcome.LOST, 0),
                ArcaneForgingFormula.resolve(3, 5, 5, false, false,
                        true, 0, true, true, true, true));
    }

    @Test
    void keepAndDowngradeRollsAreIndependent() {
        assertEquals(new ArcaneForgingFormula.Result(
                ArcaneForgingFormula.Outcome.DOWNGRADED, 2),
                ArcaneForgingFormula.resolve(3, 5, 5, false, false,
                        true, 4, true, true, true, false));
        assertEquals(new ArcaneForgingFormula.Result(
                ArcaneForgingFormula.Outcome.KEPT, 3),
                ArcaneForgingFormula.resolve(3, 5, 5, false, false,
                        true, 4, true, true, true, true));
    }

    @Test
    void unsafeAndBypassRulesMatchUpstream() {
        assertEquals(8, ArcaneForgingFormula.resolve(8, 5, 5, true, false,
                true, 8, true, true, true, true).level());
        assertEquals(8, ArcaneForgingFormula.resolve(8, 5, 5, false, true,
                true, 0, false, false, true, false).level());
    }

    @Test
    void configuredCapCanRemainAboveVanillaAfterPerfectKeep() {
        assertEquals(new ArcaneForgingFormula.Result(
                ArcaneForgingFormula.Outcome.KEPT, 7),
                ArcaneForgingFormula.resolve(8, 5, 7, false, false,
                        true, 8, true, true, true, true));
    }
}

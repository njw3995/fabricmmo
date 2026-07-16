package io.github.njw3995.fabricmmo.core.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.progression.FormulaType;
import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.api.progression.XpCurve;
import org.junit.jupiter.api.Test;

class ProgressionFormulaTest {
    private final ProgressionFormula formula = new ProgressionFormula(XpCurve.upstreamDefaults());

    @Test
    void matchesUpstreamRetroLinearDefaults() {
        assertEquals(1020, formula.xpToNextLevel(0, ProgressionMode.RETRO, FormulaType.LINEAR));
        assertEquals(1040, formula.xpToNextLevel(1, ProgressionMode.RETRO, FormulaType.LINEAR));
        assertEquals(3020, formula.xpToNextLevel(100, ProgressionMode.RETRO, FormulaType.LINEAR));
    }

    @Test
    void matchesUpstreamStandardSummation() {
        int expected = 0;
        for (int retroLevel = 1; retroLevel <= 10; retroLevel++) {
            expected += 1020 + retroLevel * 20;
        }
        assertEquals(expected,
                formula.xpToNextLevel(0, ProgressionMode.STANDARD, FormulaType.LINEAR));
    }

    @Test
    void matchesUpstreamExponentialFlooring() {
        assertEquals(2000,
                formula.xpToNextLevel(0, ProgressionMode.RETRO, FormulaType.EXPONENTIAL));
        assertEquals((int) Math.floor(0.1 * Math.pow(100, 1.8) + 2000),
                formula.xpToNextLevel(100, ProgressionMode.RETRO, FormulaType.EXPONENTIAL));
    }
}

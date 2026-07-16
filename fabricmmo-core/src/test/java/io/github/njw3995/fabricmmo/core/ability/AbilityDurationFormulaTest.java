package io.github.njw3995.fabricmmo.core.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AbilityDurationFormulaTest {
    @Test
    void matchesPinnedStandardAndRetroDefaults() {
        assertEquals(Duration.ofSeconds(2),
                AbilityDurationFormula.baseDuration(0, ProgressionMode.STANDARD, 0));
        assertEquals(Duration.ofSeconds(3),
                AbilityDurationFormula.baseDuration(5, ProgressionMode.STANDARD, 0));
        assertEquals(Duration.ofSeconds(22),
                AbilityDurationFormula.baseDuration(100, ProgressionMode.STANDARD, 0));
        assertEquals(Duration.ofSeconds(22),
                AbilityDurationFormula.baseDuration(500, ProgressionMode.STANDARD, 0));

        assertEquals(Duration.ofSeconds(3),
                AbilityDurationFormula.baseDuration(50, ProgressionMode.RETRO, 0));
        assertEquals(Duration.ofSeconds(22),
                AbilityDurationFormula.baseDuration(1000, ProgressionMode.RETRO, 0));
        assertEquals(Duration.ofSeconds(22),
                AbilityDurationFormula.baseDuration(5000, ProgressionMode.RETRO, 0));
    }

    @Test
    void honorsAbilitySpecificMaximumAfterBaseCalculation() {
        assertEquals(Duration.ofSeconds(12),
                AbilityDurationFormula.baseDuration(100, ProgressionMode.STANDARD, 12));
        assertEquals(Duration.ofSeconds(22),
                AbilityDurationFormula.baseDuration(100, ProgressionMode.STANDARD, 0));
    }

    @Test
    void supportsUncappedCustomCurvesAndRejectsInvalidInputs() {
        assertEquals(Duration.ofSeconds(102),
                AbilityDurationFormula.baseDuration(1000, 0, 10, 0));
        assertThrows(IllegalArgumentException.class,
                () -> AbilityDurationFormula.baseDuration(-1, ProgressionMode.RETRO, 0));
        assertThrows(IllegalArgumentException.class,
                () -> AbilityDurationFormula.baseDuration(1, 100, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> AbilityDurationFormula.baseDuration(1, 100, 5, -1));
    }
}

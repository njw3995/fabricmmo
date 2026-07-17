package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class SuperBreakerSpeedTest {
    @Test
    void addsTheExactDifferenceBetweenVanillaEfficiencyContributions() {
        assertEquals(26.0D, SuperBreakerSpeed.additionalMiningEfficiency(0, 5));
        assertEquals(55.0D, SuperBreakerSpeed.additionalMiningEfficiency(3, 5));
        assertEquals(0.0D, SuperBreakerSpeed.additionalMiningEfficiency(3, 0));
    }

    @Test
    void clampsNegativeInputs() {
        assertEquals(10.0D, SuperBreakerSpeed.additionalMiningEfficiency(-2, 3));
        assertEquals(0.0D, SuperBreakerSpeed.additionalMiningEfficiency(3, -2));
    }
}

package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RollXpThrottleTest {
    @Test
    void firstRollPaysAndRapidRollsLengthenCooldown() {
        RollXpThrottle throttle = new RollXpThrottle();
        assertTrue(throttle.tryConsume(1_000L, true));
        assertFalse(throttle.tryConsume(2_000L, true));
        long afterFirstPenalty = throttle.cooldownUntil();
        assertFalse(throttle.tryConsume(2_001L, true));
        assertTrue(throttle.cooldownUntil() > afterFirstPenalty);
    }

    @Test
    void disabledExploitPreventionAlwaysPays() {
        RollXpThrottle throttle = new RollXpThrottle();
        assertTrue(throttle.tryConsume(1_000L, false));
        assertTrue(throttle.tryConsume(1_001L, false));
    }
}

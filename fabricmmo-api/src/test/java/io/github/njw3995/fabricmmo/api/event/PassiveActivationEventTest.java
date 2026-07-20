package io.github.njw3995.fabricmmo.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PassiveActivationEventTest {
    @Test
    void supportsCancellationAndProbabilityMultipliers() {
        PassiveActivationEvent event = new PassiveActivationEvent(
                UUID.randomUUID(), NamespacedId.parse("fabricmmo:archery_daze"), 0.40D);
        event.resultMultiplier(1.333D);
        assertEquals(0.5332D, event.resultingProbability(), 1.0E-9D);
        event.cancel();
        assertTrue(event.cancelled());
    }

    @Test
    void clampsTheResultAndRejectsInvalidValues() {
        PassiveActivationEvent event = new PassiveActivationEvent(
                UUID.randomUUID(), NamespacedId.parse("fabricmmo:test"), 0.75D);
        event.resultMultiplier(2.0D);
        assertEquals(1.0D, event.resultingProbability());
        assertThrows(IllegalArgumentException.class, () -> event.resultMultiplier(-1.0D));
        assertThrows(IllegalArgumentException.class, () -> new PassiveActivationEvent(
                UUID.randomUUID(), NamespacedId.parse("fabricmmo:test"), 1.1D));
    }
}

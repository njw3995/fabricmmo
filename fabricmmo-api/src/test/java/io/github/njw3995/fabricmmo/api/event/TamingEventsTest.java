package io.github.njw3995.fabricmmo.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TamingEventsTest {
    @Test
    void tameEventSupportsValidatedXpMutationAndCancellation() {
        TamingEntityTamedEvent event = new TamingEntityTamedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "wolf", 250.0D);
        event.setXp(500.0D);
        event.cancel();
        assertEquals(500.0D, event.xp());
        assertTrue(event.cancelled());
        assertThrows(IllegalArgumentException.class, () -> event.setXp(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> event.setXp(-1.0D));
    }

    @Test
    void summonEventExposesLifetimeAndCanBeCancelled() {
        TamingSummonEvent event = new TamingSummonEvent(UUID.randomUUID(), UUID.randomUUID(),
                "WOLF", Duration.ofSeconds(240));
        event.cancel();
        assertEquals(Duration.ofSeconds(240), event.lifetime());
        assertTrue(event.cancelled());
        assertThrows(IllegalArgumentException.class, () -> new TamingSummonEvent(
                UUID.randomUUID(), UUID.randomUUID(), "WOLF", Duration.ofSeconds(-1)));
    }
}

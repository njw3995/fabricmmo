package io.github.njw3995.fabricmmo.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlchemyEventsTest {
    @Test
    void catalysisEventSupportsValidatedSpeedMutation() {
        AlchemyCatalysisEvent event = new AlchemyCatalysisEvent(
                UUID.randomUUID(), "minecraft:overworld", 42L, 2.5D);
        event.setSpeed(4.0D);
        event.cancel();
        assertEquals(4.0D, event.speed());
        assertTrue(event.cancelled());
        assertThrows(IllegalArgumentException.class, () -> event.setSpeed(0.0D));
        assertThrows(IllegalArgumentException.class, () -> event.setSpeed(Double.NaN));
    }

    @Test
    void brewEventExposesRecipeOutputsAndCancellation() {
        AlchemyBrewEvent event = new AlchemyBrewEvent(
                UUID.randomUUID(), "minecraft:overworld", 99L, "minecraft:fern",
                List.of("POTION_OF_SATURATION", "SPLASH_POTION_OF_SATURATION"));
        event.cancel();
        assertEquals("minecraft:fern", event.ingredientId());
        assertEquals(2, event.outputPotionIds().size());
        assertTrue(event.cancelled());
    }
}

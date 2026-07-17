package io.github.njw3995.fabricmmo.core.block;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NaturalGrowthTrackerTest {
    @Test
    void recognizesCropAndAmethystGrowth() {
        assertTrue(NaturalGrowthTracker.isGrowthTransition(
                "minecraft:wheat",
                Map.of("age", 2),
                false,
                "minecraft:wheat",
                Map.of("age", 3)));
        assertTrue(NaturalGrowthTracker.isGrowthTransition(
                "minecraft:small_amethyst_bud",
                Map.of(),
                false,
                "minecraft:medium_amethyst_bud",
                Map.of()));
    }

    @Test
    void doesNotTreatUnrelatedRandomTickMutationAsGrowth() {
        assertFalse(NaturalGrowthTracker.isGrowthTransition(
                "minecraft:copper_block",
                Map.of(),
                false,
                "minecraft:exposed_copper",
                Map.of()));
    }
}

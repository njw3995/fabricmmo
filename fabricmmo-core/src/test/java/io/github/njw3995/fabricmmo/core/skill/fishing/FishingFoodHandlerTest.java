package io.github.njw3995.fabricmmo.core.skill.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class FishingFoodHandlerTest {
    @Test
    void matchesUpstreamFishermansDietFoodSet() {
        assertEquals(Set.of(
                "minecraft:cod",
                "minecraft:cooked_cod",
                "minecraft:salmon",
                "minecraft:cooked_salmon",
                "minecraft:tropical_fish"),
                FishingFoodRules.fishermansDietFoodIds());
    }
}

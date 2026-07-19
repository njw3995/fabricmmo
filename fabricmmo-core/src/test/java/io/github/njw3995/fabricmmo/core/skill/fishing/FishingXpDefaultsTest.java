package io.github.njw3995.fabricmmo.core.skill.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import org.junit.jupiter.api.Test;

class FishingXpDefaultsTest {
    @Test
    void matchesMinecraft1211ApplicableUpstreamFishXp() {
        assertEquals(4, FishingXpDefaults.values().size());
        assertEquals(100, FishingXpDefaults.values().get(id("cod")).intValue());
        assertEquals(600, FishingXpDefaults.values().get(id("salmon")).intValue());
        assertEquals(10_000, FishingXpDefaults.values().get(id("tropical_fish")).intValue());
        assertEquals(2_400, FishingXpDefaults.values().get(id("pufferfish")).intValue());
        assertEquals(50, FishingXpDefaults.shakeXp());
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("minecraft", path);
    }
}

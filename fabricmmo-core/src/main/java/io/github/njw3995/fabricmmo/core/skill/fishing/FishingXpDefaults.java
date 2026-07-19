package io.github.njw3995.fabricmmo.core.skill.fishing;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;

/** Minecraft 1.21.1-applicable mcMMO 2.3.000 Fishing XP defaults. */
public final class FishingXpDefaults {
    private static final Map<NamespacedId, Integer> VALUES = Map.of(
            id("cod"), 100,
            id("salmon"), 600,
            id("tropical_fish"), 10_000,
            id("pufferfish"), 2_400);

    private FishingXpDefaults() {
    }

    public static Map<NamespacedId, Integer> values() {
        return VALUES;
    }

    public static int shakeXp() {
        return 50;
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("minecraft", path);
    }
}

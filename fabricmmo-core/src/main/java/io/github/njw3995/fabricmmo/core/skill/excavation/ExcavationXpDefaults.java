package io.github.njw3995.fabricmmo.core.skill.excavation;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minecraft 1.21.1-applicable Excavation XP defaults from mcMMO 2.3.000. */
public final class ExcavationXpDefaults {
    private ExcavationXpDefaults() {
    }

    public static Map<NamespacedId, Integer> values() {
        LinkedHashMap<NamespacedId, Integer> values = new LinkedHashMap<>();
        put(values, "clay", 40);
        put(values, "dirt", 40);
        put(values, "rooted_dirt", 60);
        put(values, "coarse_dirt", 40);
        put(values, "podzol", 40);
        put(values, "grass_block", 40);
        put(values, "gravel", 40);
        put(values, "mycelium", 40);
        put(values, "sand", 40);
        put(values, "red_sand", 40);
        put(values, "snow", 20);
        put(values, "snow_block", 40);
        put(values, "soul_sand", 40);
        put(values, "soul_soil", 40);
        put(values, "mud", 80);
        put(values, "muddy_mangrove_roots", 90);
        return Map.copyOf(values);
    }

    private static void put(Map<NamespacedId, Integer> values, String path, int xp) {
        values.put(new NamespacedId("minecraft", path), xp);
    }
}

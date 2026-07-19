package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minecraft 1.21.1-applicable XP defaults from mcMMO 2.3.000 experience.yml. */
public final class WoodcuttingXpDefaults {
    private static final Map<NamespacedId, Integer> VALUES = create();

    private WoodcuttingXpDefaults() {
    }

    public static Map<NamespacedId, Integer> values() {
        return VALUES;
    }

    private static Map<NamespacedId, Integer> create() {
        LinkedHashMap<NamespacedId, Integer> values = new LinkedHashMap<>();
        add(values, "crimson_hyphae", 50);
        add(values, "stripped_crimson_hyphae", 50);
        add(values, "warped_hyphae", 50);
        add(values, "stripped_warped_hyphae", 50);
        add(values, "nether_wart_block", 1);
        add(values, "warped_wart_block", 1);
        add(values, "shroomlight", 100);
        add(values, "crimson_stem", 35);
        add(values, "warped_stem", 35);
        add(values, "oak_log", 70);
        add(values, "cherry_log", 105);
        add(values, "spruce_log", 80);
        add(values, "birch_log", 90);
        add(values, "jungle_log", 100);
        add(values, "acacia_log", 90);
        add(values, "dark_oak_log", 90);
        add(values, "stripped_oak_log", 70);
        add(values, "stripped_cherry_log", 105);
        add(values, "stripped_spruce_log", 80);
        add(values, "stripped_birch_log", 90);
        add(values, "stripped_jungle_log", 100);
        add(values, "stripped_acacia_log", 90);
        add(values, "stripped_dark_oak_log", 90);
        add(values, "stripped_oak_wood", 70);
        add(values, "stripped_cherry_wood", 70);
        add(values, "stripped_spruce_wood", 80);
        add(values, "stripped_birch_wood", 90);
        add(values, "stripped_jungle_wood", 100);
        add(values, "stripped_acacia_wood", 90);
        add(values, "stripped_dark_oak_wood", 90);
        add(values, "stripped_mangrove_log", 110);
        add(values, "stripped_crimson_stem", 50);
        add(values, "stripped_warped_stem", 50);
        add(values, "oak_wood", 70);
        add(values, "cherry_wood", 105);
        add(values, "spruce_wood", 70);
        add(values, "birch_wood", 70);
        add(values, "jungle_wood", 70);
        add(values, "acacia_wood", 70);
        add(values, "dark_oak_wood", 70);
        add(values, "mangrove_wood", 80);
        add(values, "mangrove_log", 95);
        add(values, "mangrove_roots", 10);
        add(values, "red_mushroom_block", 70);
        add(values, "brown_mushroom_block", 70);
        add(values, "mushroom_stem", 80);
        return Map.copyOf(values);
    }

    private static void add(Map<NamespacedId, Integer> values, String path, int xp) {
        values.put(new NamespacedId("minecraft", path), xp);
    }
}

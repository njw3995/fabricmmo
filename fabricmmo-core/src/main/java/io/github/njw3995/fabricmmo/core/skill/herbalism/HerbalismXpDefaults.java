package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minecraft 1.21.1-applicable mcMMO 2.3.000 Herbalism XP defaults. */
public final class HerbalismXpDefaults {
    private HerbalismXpDefaults() {
    }

    public static Map<NamespacedId, Integer> values() {
        LinkedHashMap<NamespacedId, Integer> values = new LinkedHashMap<>();
        add(values, 160, "pitcher_plant");
        add(values, 10, "pink_petals");
        add(values, 140, "small_dripleaf", "big_dripleaf");
        add(values, 90, "cave_vines", "cave_vines_plant");
        add(values, 1, "glow_lichen");
        add(values, 150, "moss_block");
        add(values, 6, "crimson_roots", "warped_roots");
        add(values, 3, "nether_wart_block", "warped_wart_block", "kelp", "kelp_plant");
        add(values, 10, "nether_sprouts", "seagrass", "tall_seagrass", "fern", "grass", "short_grass", "vine", "bamboo");
        add(values, 50, "crimson_fungus", "warped_fungus", "sweet_berry_bush", "carrots", "potatoes", "wheat", "beetroots", "nether_wart", "lilac", "peony", "rose_bush", "sunflower", "tall_grass", "large_fern");
        add(values, 250, "shroomlight");
        add(values, 200, "bee_nest");
        add(values, 80, "tube_coral", "tube_coral_fan", "tube_coral_wall_fan");
        add(values, 90, "brain_coral", "brain_coral_fan", "brain_coral_wall_fan");
        add(values, 75, "bubble_coral", "bubble_coral_fan", "bubble_coral_wall_fan");
        add(values, 120, "fire_coral", "fire_coral_fan", "fire_coral_wall_fan");
        add(values, 175, "horn_coral", "horn_coral_fan", "horn_coral_wall_fan");
        add(values, 10, "dead_tube_coral", "dead_brain_coral", "dead_bubble_coral", "dead_fire_coral", "dead_horn_coral", "dead_tube_coral_fan", "dead_brain_coral_fan", "dead_bubble_coral_fan", "dead_fire_coral_fan", "dead_horn_coral_fan", "dead_tube_coral_wall_fan", "dead_brain_coral_wall_fan", "dead_bubble_coral_wall_fan", "dead_fire_coral_wall_fan", "dead_horn_coral_wall_fan");
        add(values, 300, "allium");
        add(values, 150, "azure_bluet", "blue_orchid", "brown_mushroom", "orange_tulip", "oxeye_daisy", "pink_tulip", "red_mushroom", "red_tulip", "white_tulip", "cornflower", "lily_of_the_valley");
        add(values, 30, "cactus", "cocoa", "dead_bush", "sugar_cane");
        add(values, 25, "chorus_flower");
        add(values, 1, "chorus_plant");
        add(values, 20, "melon", "pumpkin");
        add(values, 100, "poppy", "lily_pad", "dandelion");
        add(values, 10, "weeping_vines_plant", "twisting_vines_plant");
        add(values, 500, "wither_rose");
        add(values, 90, "torchflower");
        return Map.copyOf(values);
    }

    private static void add(Map<NamespacedId, Integer> values, int xp, String... paths) {
        for (String path : paths) {
            values.put(new NamespacedId("minecraft", path), xp);
        }
    }
}

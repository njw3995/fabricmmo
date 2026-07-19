package io.github.njw3995.fabricmmo.core.skill.fishing;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

/** Pure Fisherman's Diet food definitions with lazy Minecraft item resolution. */
final class FishingFoodRules {
    private static final List<FoodDefinition> FOODS = List.of(
            new FoodDefinition("minecraft:cod", () -> Items.COD),
            new FoodDefinition("minecraft:cooked_cod", () -> Items.COOKED_COD),
            new FoodDefinition("minecraft:salmon", () -> Items.SALMON),
            new FoodDefinition("minecraft:cooked_salmon", () -> Items.COOKED_SALMON),
            new FoodDefinition("minecraft:tropical_fish", () -> Items.TROPICAL_FISH));
    private static final Set<String> FOOD_IDS = FOODS.stream()
            .map(FoodDefinition::id)
            .collect(Collectors.toUnmodifiableSet());

    private FishingFoodRules() {
    }

    static boolean isFishermansDietFood(Item item) {
        for (FoodDefinition food : FOODS) {
            if (food.item().get() == item) {
                return true;
            }
        }
        return false;
    }

    static Set<String> fishermansDietFoodIds() {
        return FOOD_IDS;
    }

    private record FoodDefinition(String id, Supplier<Item> item) {
    }
}

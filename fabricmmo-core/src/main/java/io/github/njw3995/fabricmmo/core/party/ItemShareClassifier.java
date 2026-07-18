package io.github.njw3995.fabricmmo.core.party;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Exact Minecraft 1.21.1-applicable port of upstream ItemUtils party-share classification. */
public final class ItemShareClassifier {
    private static final Set<String> MINING = Set.of(
            "coal", "coal_ore", "diamond", "diamond_ore", "emerald", "emerald_ore",
            "gold_ore", "iron_ore", "lapis_ore", "redstone_ore", "redstone",
            "glowstone_dust", "quartz", "nether_quartz_ore", "lapis_lazuli");

    private static final Set<String> HERBALISM = Set.of(
            "wheat", "wheat_seeds", "carrot", "chorus_fruit", "chorus_flower", "potato",
            "beetroot", "beetroot_seeds", "nether_wart", "brown_mushroom", "red_mushroom",
            "rose_bush", "dandelion", "cactus", "sugar_cane", "melon", "melon_seeds",
            "pumpkin", "pumpkin_seeds", "lily_pad", "vine", "tall_grass", "cocoa_beans");

    private static final Set<String> LOOT = Set.of(
            "string", "feather", "chicken", "cooked_chicken", "leather", "beef",
            "cooked_beef", "porkchop", "cooked_porkchop", "white_wool", "black_wool",
            "blue_wool", "brown_wool", "cyan_wool", "gray_wool", "green_wool",
            "light_blue_wool", "light_gray_wool", "lime_wool", "magenta_wool", "orange_wool",
            "pink_wool", "purple_wool", "red_wool", "yellow_wool", "iron_ingot", "snowball",
            "blaze_rod", "spider_eye", "gunpowder", "ender_pearl", "ghast_tear", "magma_cream",
            "bone", "arrow", "slime_ball", "nether_star", "rotten_flesh", "gold_nugget",
            "egg", "rose_bush", "coal");

    private static final Set<String> WOODCUTTING = Set.of(
            "acacia_log", "birch_log", "dark_oak_log", "jungle_log", "oak_log", "spruce_log",
            "mangrove_log", "cherry_log", "stripped_acacia_log", "stripped_birch_log",
            "stripped_dark_oak_log", "stripped_jungle_log", "stripped_oak_log",
            "stripped_spruce_log", "stripped_mangrove_log", "stripped_cherry_log",
            "acacia_sapling", "spruce_sapling", "birch_sapling", "dark_oak_sapling",
            "jungle_sapling", "oak_sapling", "acacia_leaves", "birch_leaves",
            "dark_oak_leaves", "jungle_leaves", "oak_leaves", "spruce_leaves",
            "bee_nest", "apple");

    private final ItemWeightSettings weights;

    public ItemShareClassifier(ItemWeightSettings weights) {
        this.weights = java.util.Objects.requireNonNull(weights, "weights");
    }

    public Optional<ItemShareCategory> classify(String itemPath) {
        String path = itemPath.toLowerCase(Locale.ROOT);
        if (LOOT.contains(path)) {
            return Optional.of(ItemShareCategory.LOOT);
        }
        if (MINING.contains(path)) {
            return Optional.of(ItemShareCategory.MINING);
        }
        if (HERBALISM.contains(path)) {
            return Optional.of(ItemShareCategory.HERBALISM);
        }
        if (WOODCUTTING.contains(path)) {
            return Optional.of(ItemShareCategory.WOODCUTTING);
        }
        if (weights.misc(path)) {
            return Optional.of(ItemShareCategory.MISC);
        }
        return Optional.empty();
    }
}

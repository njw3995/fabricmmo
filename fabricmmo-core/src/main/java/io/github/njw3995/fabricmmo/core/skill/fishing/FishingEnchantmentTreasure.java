package io.github.njw3995.fabricmmo.core.skill.fishing;

import io.github.njw3995.fabricmmo.api.NamespacedId;

public record FishingEnchantmentTreasure(
        NamespacedId enchantmentId,
        int level,
        FishingRarity rarity) {
    public FishingEnchantmentTreasure {
        if (level < 1) {
            throw new IllegalArgumentException("Fishing enchantment level must be positive");
        }
    }
}

package io.github.njw3995.fabricmmo.core.skill.fishing;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Set;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public record FishingTreasure(
        Item item,
        int amount,
        int xp,
        FishingRarity rarity,
        Set<NamespacedId> enchantmentWhitelist,
        Set<NamespacedId> enchantmentBlacklist) {
    public FishingTreasure {
        if (amount < 1 || xp < 0) {
            throw new IllegalArgumentException("Fishing treasure amount and XP must be valid");
        }
        enchantmentWhitelist = Set.copyOf(enchantmentWhitelist);
        enchantmentBlacklist = Set.copyOf(enchantmentBlacklist);
    }

    public FishingTreasure(Item item, int amount, int xp, FishingRarity rarity) {
        this(item, amount, xp, rarity, Set.of(), Set.of());
    }

    public boolean enchantedBook() {
        return item == Items.ENCHANTED_BOOK;
    }

    public boolean allowsEnchantment(NamespacedId enchantmentId) {
        if (!enchantmentWhitelist.isEmpty()) {
            return enchantmentWhitelist.contains(enchantmentId);
        }
        return enchantmentBlacklist.isEmpty() || !enchantmentBlacklist.contains(enchantmentId);
    }
}

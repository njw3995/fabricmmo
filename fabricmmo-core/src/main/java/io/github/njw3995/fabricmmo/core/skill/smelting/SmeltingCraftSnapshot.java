package io.github.njw3995.fabricmmo.core.skill.smelting;

import net.minecraft.item.ItemStack;

/** Furnace state captured immediately before a successful vanilla cooking operation. */
public record SmeltingCraftSnapshot(ItemStack source, int resultAmountBefore) {
    public SmeltingCraftSnapshot {
        source = source.copy();
        if (source.isEmpty() || resultAmountBefore < 0) {
            throw new IllegalArgumentException("Invalid furnace craft snapshot");
        }
    }
}

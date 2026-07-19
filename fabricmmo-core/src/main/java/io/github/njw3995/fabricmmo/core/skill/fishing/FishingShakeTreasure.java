package io.github.njw3995.fabricmmo.core.skill.fishing;

import net.minecraft.item.Item;

public record FishingShakeTreasure(
        Item item,
        int amount,
        double chancePercent,
        int dropLevel,
        String potionType,
        boolean inventorySteal,
        boolean wholeStacks) {
    public FishingShakeTreasure {
        if (amount < 1 || chancePercent < 0.0D || dropLevel < 0) {
            throw new IllegalArgumentException("Invalid Shake treasure");
        }
        potionType = potionType == null ? "" : potionType;
    }

    public FishingShakeTreasure(
            Item item,
            int amount,
            double chancePercent,
            int dropLevel,
            String potionType) {
        this(item, amount, chancePercent, dropLevel, potionType, false, false);
    }

    public static FishingShakeTreasure inventory(
            double chancePercent,
            int dropLevel,
            boolean wholeStacks) {
        return new FishingShakeTreasure(
                net.minecraft.item.Items.BEDROCK,
                1,
                chancePercent,
                dropLevel,
                "",
                true,
                wholeStacks);
    }
}

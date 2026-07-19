package io.github.njw3995.fabricmmo.core.skill.fishing;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Deterministic mcMMO Fishing treasure and Shake selection formulas. */
public final class FishingTreasureRoller {
    private final FishingTreasureTable table;

    public FishingTreasureRoller(FishingTreasureTable table) {
        this.table = Objects.requireNonNull(table, "table");
    }

    public Optional<TreasureRoll> rollTreasure(
            int tier,
            int luckOfTheSeaLevel,
            double lureModifierPercent,
            double rollZeroToOne,
            int selection) {
        double dice = checkedRoll(rollZeroToOne) * 100.0D;
        dice *= 1.0D - Math.max(0, luckOfTheSeaLevel) * lureModifierPercent / 100.0D;
        for (FishingRarity rarity : FishingRarity.values()) {
            double dropRate = table.itemRate(tier, rarity);
            if (dice <= dropRate) {
                List<FishingTreasure> choices = table.treasures(rarity);
                if (choices.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(new TreasureRoll(
                        choices.get(Math.floorMod(selection, choices.size())), rarity));
            }
            dice -= dropRate;
        }
        return Optional.empty();
    }

    public Optional<FishingRarity> rollEnchantmentRarity(
            int tier,
            double rollZeroToOne) {
        double dice = checkedRoll(rollZeroToOne) * 100.0D;
        for (FishingRarity rarity : FishingRarity.values()) {
            double dropRate = table.enchantmentRate(tier, rarity);
            if (dice <= dropRate) {
                return Optional.of(rarity);
            }
            dice -= dropRate;
        }
        return Optional.empty();
    }

    public Optional<FishingShakeTreasure> rollShake(
            String entityPath,
            int rollZeroToNinetyNine) {
        if (rollZeroToNinetyNine < 0 || rollZeroToNinetyNine >= 100) {
            throw new IllegalArgumentException("Shake roll must be in [0, 100)");
        }
        double cumulative = 0.0D;
        for (FishingShakeTreasure treasure : table.shake(entityPath)) {
            cumulative += treasure.chancePercent();
            if (rollZeroToNinetyNine < cumulative) {
                return Optional.of(treasure);
            }
        }
        return Optional.empty();
    }

    public double totalEnchantmentChance(int tier) {
        double result = 0.0D;
        for (FishingRarity rarity : FishingRarity.values()) {
            if (rarity != FishingRarity.MYTHIC) {
                result += table.enchantmentRate(tier, rarity);
            }
        }
        return result >= 1.0D ? result : 0.0D;
    }

    private static double checkedRoll(double value) {
        if (Double.isNaN(value) || value < 0.0D || value >= 1.0D) {
            throw new IllegalArgumentException("rollZeroToOne must be in [0, 1)");
        }
        return value;
    }

    public record TreasureRoll(FishingTreasure treasure, FishingRarity rarity) {
    }
}

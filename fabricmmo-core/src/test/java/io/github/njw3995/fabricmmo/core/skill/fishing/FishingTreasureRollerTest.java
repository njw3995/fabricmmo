package io.github.njw3995.fabricmmo.core.skill.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FishingTreasureRollerTest {
    @Test
    void treasureRarityRollsSequentiallyAndLuckScalesTheDice() {
        FishingTreasure common = new FishingTreasure(null, 1, 200, FishingRarity.COMMON);
        FishingTreasure rare = new FishingTreasure(null, 1, 800, FishingRarity.RARE);
        FishingTreasureRoller roller = new FishingTreasureRoller(table(
                Map.of(
                        FishingRarity.COMMON, List.of(common),
                        FishingRarity.RARE, List.of(rare)),
                Map.of(
                        FishingRarity.COMMON, 10.0D,
                        FishingRarity.RARE, 5.0D),
                Map.of(),
                Map.of(),
                Map.of()));

        assertEquals(FishingRarity.COMMON,
                roller.rollTreasure(1, 0, 4.0D, 0.05D, 0).orElseThrow().rarity());
        assertEquals(FishingRarity.RARE,
                roller.rollTreasure(1, 0, 4.0D, 0.12D, 0).orElseThrow().rarity());
        assertTrue(roller.rollTreasure(1, 0, 4.0D, 0.20D, 0).isEmpty());
        assertEquals(FishingRarity.COMMON,
                roller.rollTreasure(1, 3, 4.0D, 0.11D, 0).orElseThrow().rarity());
    }

    @Test
    void shakeUsesUpstreamIntegerRollAndIgnoresConfiguredDropLevel() {
        FishingShakeTreasure first = new FishingShakeTreasure(null, 1, 49.0D, 0, "");
        FishingShakeTreasure second = new FishingShakeTreasure(null, 1, 1.0D, 20, "");
        FishingTreasureRoller roller = new FishingTreasureRoller(table(
                Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of("cave_spider", List.of(first, second))));

        assertEquals(first, roller.rollShake("cave_spider", 0).orElseThrow());
        assertEquals(first, roller.rollShake("cave_spider", 48).orElseThrow());
        assertEquals(second, roller.rollShake("cave_spider", 49).orElseThrow());
        assertTrue(roller.rollShake("cave_spider", 50).isEmpty());
    }

    @Test
    void commandChanceExcludesMythicAndSuppressesSubOnePercentTotals() {
        FishingTreasureRoller visible = new FishingTreasureRoller(table(
                Map.of(), Map.of(), Map.of(),
                Map.of(FishingRarity.COMMON, 0.75D, FishingRarity.MYTHIC, 99.0D),
                Map.of()));
        FishingTreasureRoller hidden = new FishingTreasureRoller(table(
                Map.of(), Map.of(), Map.of(),
                Map.of(FishingRarity.COMMON, 0.50D, FishingRarity.RARE, 0.25D),
                Map.of()));
        FishingTreasureRoller shown = new FishingTreasureRoller(table(
                Map.of(), Map.of(), Map.of(),
                Map.of(FishingRarity.COMMON, 0.75D, FishingRarity.RARE, 0.50D,
                        FishingRarity.MYTHIC, 99.0D),
                Map.of()));

        assertEquals(0.0D, visible.totalEnchantmentChance(1));
        assertEquals(0.0D, hidden.totalEnchantmentChance(1));
        assertEquals(1.25D, shown.totalEnchantmentChance(1));
    }

    @Test
    void staticProbabilityUsesUpstreamLuckyMultiplierAndInclusiveBoundary() {
        assertEquals(99.975D, FishingProbability.luckyPercent(75.0D, true));
        assertTrue(FishingProbability.succeeds(0.75D, 75.0D));
    }

    private static FishingTreasureTable table(
            Map<FishingRarity, List<FishingTreasure>> treasures,
            Map<FishingRarity, Double> itemRates,
            Map<FishingRarity, List<FishingEnchantmentTreasure>> enchantments,
            Map<FishingRarity, Double> enchantmentRates,
            Map<String, List<FishingShakeTreasure>> shake) {
        EnumMap<FishingRarity, List<FishingTreasure>> treasureMap = new EnumMap<>(FishingRarity.class);
        treasureMap.putAll(treasures);
        EnumMap<FishingRarity, List<FishingEnchantmentTreasure>> enchantmentMap =
                new EnumMap<>(FishingRarity.class);
        enchantmentMap.putAll(enchantments);
        EnumMap<FishingRarity, Double> itemRateMap = new EnumMap<>(FishingRarity.class);
        itemRateMap.putAll(itemRates);
        EnumMap<FishingRarity, Double> enchantmentRateMap =
                new EnumMap<>(FishingRarity.class);
        enchantmentRateMap.putAll(enchantmentRates);
        return new FishingTreasureTable(
                treasureMap, Map.of(1, itemRateMap),
                enchantmentMap, Map.of(1, enchantmentRateMap), shake);
    }
}

package io.github.njw3995.fabricmmo.core.party;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartyShareAllocatorTest {
    @Test
    void randomModeAllocatesEveryItemToAnEligiblePlayer() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PartyShareAllocator allocator = new PartyShareAllocator(new Random(3995L));
        List<UUID> winners = allocator.allocate(
                ShareMode.RANDOM, List.of(first, second), 64, 10);
        assertEquals(64, winners.size());
        assertTrue(winners.stream().allMatch(id -> id.equals(first) || id.equals(second)));
        assertTrue(winners.contains(first));
        assertTrue(winners.contains(second));
    }

    @Test
    void equalModeMaintainsBoundedPositiveModifiers() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PartyShareAllocator allocator = new PartyShareAllocator(new Random(191L));
        List<UUID> winners = allocator.allocate(
                ShareMode.EQUAL, List.of(first, second), 128, 5);
        assertEquals(128, winners.size());
        assertTrue(winners.contains(first));
        assertTrue(winners.contains(second));
        assertTrue(allocator.modifier(first) >= 10);
        assertTrue(allocator.modifier(second) >= 10);
    }
    @Test
    void classifierMatchesUpstreamMaterialListsWithoutModernizedAdditions() {
        ItemShareClassifier classifier = new ItemShareClassifier(
                ItemWeightSettings.upstreamDefaults());
        assertEquals(ItemShareCategory.MINING, classifier.classify("diamond").orElseThrow());
        assertEquals(ItemShareCategory.LOOT, classifier.classify("iron_ingot").orElseThrow());
        assertTrue(classifier.classify("sweet_berries").isEmpty());
        assertTrue(classifier.classify("deepslate_diamond_ore").isEmpty());
    }

}

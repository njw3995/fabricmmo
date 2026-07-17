package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MiningDropQuantityModifierTest {
    @Test
    void addsUpstreamBonusQuantityOnlyToEnabledActualDrops() {
        MiningDropSettings settings = settings(Set.of(
                NamespacedId.parse("minecraft:diamond")));
        List<MiningDropStack> original = List.of(
                new MiningDropStack(NamespacedId.parse("minecraft:diamond"), 4, 64, false));

        List<MiningDropStack> result = MiningDropQuantityModifier.apply(
                original, settings, MiningDropOutcome.TRIPLE);

        assertNotSame(original, result);
        assertEquals(6, result.getFirst().count());
        assertEquals(4, original.getFirst().count());
    }

    @Test
    void capsBonusQuantityAtTheVanillaStackMaximum() {
        MiningDropSettings settings = settings(Set.of(
                NamespacedId.parse("minecraft:redstone")));

        List<MiningDropStack> result = MiningDropQuantityModifier.apply(
                List.of(new MiningDropStack(
                        NamespacedId.parse("minecraft:redstone"), 64, 64, false)),
                settings,
                MiningDropOutcome.DOUBLE);

        assertEquals(64, result.getFirst().count());
    }

    @Test
    void protectsPotentialContainerContentsLikeUpstream() {
        MiningDropSettings settings = settings(Set.of(
                NamespacedId.parse("minecraft:chest"),
                NamespacedId.parse("minecraft:diamond")));
        List<MiningDropStack> mixed = List.of(
                new MiningDropStack(NamespacedId.parse("minecraft:chest"), 1, 64, true),
                new MiningDropStack(NamespacedId.parse("minecraft:diamond"), 10, 64, false));

        List<MiningDropStack> result = MiningDropQuantityModifier.apply(
                mixed, settings, MiningDropOutcome.DOUBLE);

        assertEquals(2, result.get(0).count());
        assertEquals(10, result.get(1).count());
    }

    @Test
    void rejectsDropListsContainingMultipleBlockItemEntities() {
        MiningDropSettings settings = settings(Set.of(
                NamespacedId.parse("minecraft:chest"),
                NamespacedId.parse("minecraft:stone")));
        List<MiningDropStack> suspicious = List.of(
                new MiningDropStack(NamespacedId.parse("minecraft:chest"), 1, 64, true),
                new MiningDropStack(NamespacedId.parse("minecraft:stone"), 1, 64, true));

        List<MiningDropStack> result = MiningDropQuantityModifier.apply(
                suspicious, settings, MiningDropOutcome.TRIPLE);

        assertEquals(suspicious, result);
    }

    private static MiningDropSettings settings(Set<NamespacedId> materials) {
        MiningDropSettings defaults = MiningDropSettings.upstreamDefaults();
        return new MiningDropSettings(
                materials,
                defaults.silkTouchEnabled(),
                defaults.allowSuperBreakerTripleDrops(),
                defaults.doubleDropsChanceMaxPercent(),
                defaults.doubleDropsMaxLevelStandard(),
                defaults.doubleDropsMaxLevelRetro(),
                defaults.doubleDropsUnlockLevelStandard(),
                defaults.doubleDropsUnlockLevelRetro(),
                defaults.motherLodeChanceMaxPercent(),
                defaults.motherLodeMaxLevelStandard(),
                defaults.motherLodeMaxLevelRetro(),
                defaults.motherLodeUnlockLevelStandard(),
                defaults.motherLodeUnlockLevelRetro());
    }
}

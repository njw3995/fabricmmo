package io.github.njw3995.fabricmmo.core.skill.axes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AxesDamageTest {
    @Test
    void axeMasteryUsesRankTimesConfiguredMultiplier() {
        assertEquals(0.0D, AxesDamage.axeMasteryDamage(0, 1.0D), 1.0E-9);
        assertEquals(4.0D, AxesDamage.axeMasteryDamage(4, 1.0D), 1.0E-9);
        assertEquals(6.0D, AxesDamage.axeMasteryDamage(4, 1.5D), 1.0E-9);
    }

    @Test
    void criticalExtraProducesPinnedPveAndPvpTotals() {
        assertEquals(10.0D,
                AxesDamage.criticalExtraDamage(10.0D, false, 1.5D, 2.0D), 1.0E-9);
        assertEquals(5.0D,
                AxesDamage.criticalExtraDamage(10.0D, true, 1.5D, 2.0D), 1.0E-9);
    }

    @Test
    void skullSplitterUsesRawPrimaryDamageAttackStrengthAndOneDamageMinimum() {
        assertEquals(4.0D, AxesDamage.skullSplitterDamage(8.0D, 2.0D, 1.0D), 1.0E-9);
        assertEquals(2.0D, AxesDamage.skullSplitterDamage(8.0D, 2.0D, 0.5D), 1.0E-9);
        assertEquals(1.0D, AxesDamage.skullSplitterDamage(1.0D, 2.0D, 0.25D), 1.0E-9);
    }

    @Test
    void armorImpactMatchesUpstreamUnbreakingReductionAndDurabilityCap() {
        assertEquals(130, AxesDamage.armorImpactDurabilityDamage(130.0D, 0, 250));
        assertEquals(91, AxesDamage.armorImpactDurabilityDamage(130.0D, 3, 250));
        assertEquals(50, AxesDamage.armorImpactDurabilityDamage(130.0D, 0, 50));
        assertEquals(0, AxesDamage.armorImpactDurabilityDamage(-1.0D, 0, 250));
    }

    @Test
    void limitBreakUsesUpstreamArmorQualityBandsAndIntegerTruncation() {
        assertEquals(2, AxesDamage.limitBreakDamage(10, 4));
        assertEquals(5, AxesDamage.limitBreakDamage(10, 8));
        assertEquals(7, AxesDamage.limitBreakDamage(10, 12));
        assertEquals(10, AxesDamage.limitBreakDamage(10, 13));
    }
}

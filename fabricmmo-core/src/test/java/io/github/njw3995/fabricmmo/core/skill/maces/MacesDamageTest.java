package io.github.njw3995.fabricmmo.core.skill.maces;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MacesDamageTest {
    @Test
    void crushUsesPinnedBasePlusRankTimesMultiplierFormula() {
        assertEquals(0.0D, MacesDamage.crushDamage(0, 0.5D, 1.0D), 1.0E-9);
        assertEquals(1.5D, MacesDamage.crushDamage(1, 0.5D, 1.0D), 1.0E-9);
        assertEquals(4.5D, MacesDamage.crushDamage(4, 0.5D, 1.0D), 1.0E-9);
    }

    @Test
    void limitBreakUsesUpstreamArmorQualityBandsAndIntegerTruncation() {
        assertEquals(2, MacesDamage.limitBreakDamage(10, 4));
        assertEquals(5, MacesDamage.limitBreakDamage(10, 8));
        assertEquals(7, MacesDamage.limitBreakDamage(10, 12));
        assertEquals(10, MacesDamage.limitBreakDamage(10, 13));
    }
}

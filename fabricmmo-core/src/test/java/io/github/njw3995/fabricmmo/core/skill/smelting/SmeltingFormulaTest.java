package io.github.njw3995.fabricmmo.core.skill.smelting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SmeltingFormulaTest {
    @Test
    void childLevelUsesFloorOfParentAverageWithoutOverflow() {
        assertEquals(12, SmeltingFormula.childLevel(10, 15));
        assertEquals(Integer.MAX_VALUE,
                SmeltingFormula.childLevel(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void fuelRanksAndShortClampMatchUpstream() {
        assertEquals(200, SmeltingFormula.fuelTime(100, 1));
        assertEquals(300, SmeltingFormula.fuelTime(100, 2));
        assertEquals(400, SmeltingFormula.fuelTime(100, 3));
        assertEquals(Short.MAX_VALUE, SmeltingFormula.fuelTime(20_000, 3));
    }

    @Test
    void secondSmeltRequiresRoomForTwoOutputs() {
        assertTrue(SmeltingFormula.hasRoomForSecondSmelt(0, 64));
        assertTrue(SmeltingFormula.hasRoomForSecondSmelt(62, 64));
        assertFalse(SmeltingFormula.hasRoomForSecondSmelt(63, 64));
    }
}

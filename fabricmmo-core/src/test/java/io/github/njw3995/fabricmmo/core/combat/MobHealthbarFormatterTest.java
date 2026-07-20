package io.github.njw3995.fabricmmo.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

class MobHealthbarFormatterTest {
    @Test
    void heartsUseCappedHalfMaxHealthAndCeilingForRemainingHealth() {
        MobHealthbarFormatter.Display full = MobHealthbarFormatter.display(
                MobHealthbarSettings.DisplayType.HEARTS, 100.0D, 100.0D);
        MobHealthbarFormatter.Display partial = MobHealthbarFormatter.display(
                MobHealthbarSettings.DisplayType.HEARTS, 20.0D, 1.0D);

        assertEquals("❤", full.symbol());
        assertEquals(Formatting.DARK_RED, full.color());
        assertEquals(10, full.coloredSymbols());
        assertEquals(0, full.graySymbols());
        assertEquals(1, partial.coloredSymbols());
        assertEquals(9, partial.graySymbols());
    }

    @Test
    void barUsesPinnedUpstreamColorThresholds() {
        assertColor(85.0D, Formatting.DARK_GREEN);
        assertColor(70.0D, Formatting.GREEN);
        assertColor(55.0D, Formatting.GOLD);
        assertColor(40.0D, Formatting.YELLOW);
        assertColor(25.0D, Formatting.RED);
        assertColor(24.9D, Formatting.DARK_RED);
    }

    private static void assertColor(double health, Formatting expected) {
        MobHealthbarFormatter.Display display = MobHealthbarFormatter.display(
                MobHealthbarSettings.DisplayType.BAR, 100.0D, health);
        assertEquals(expected, display.color());
    }
}

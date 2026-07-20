package io.github.njw3995.fabricmmo.core.skill.swords;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuptureTickerTest {
    @Test
    void pinnedFiveSecondTaskDamagesEveryTenTicksAndExpiresBeforeTickOneHundredDamage() {
        RuptureTicker ticker = new RuptureTicker(100);
        List<Integer> damageTicks = new ArrayList<>();
        List<Integer> animationTicks = new ArrayList<>();

        for (int tick = 1; tick <= 100; tick++) {
            RuptureTicker.Step step = ticker.tick();
            if (step == RuptureTicker.Step.DAMAGE
                    || step == RuptureTicker.Step.DAMAGE_AND_ANIMATE) {
                damageTicks.add(tick);
            }
            if (step == RuptureTicker.Step.DAMAGE_AND_ANIMATE) {
                animationTicks.add(tick);
            }
            if (tick == 100) {
                assertEquals(RuptureTicker.Step.EXPIRED, step);
            }
        }

        assertEquals(List.of(10, 20, 30, 40, 50, 60, 70, 80, 90), damageTicks);
        assertEquals(List.of(10, 30, 50, 70, 90), animationTicks);
    }

    @Test
    void refreshForcesDamageOnNextTickWithoutResettingFailsafeLifetime() {
        RuptureTicker ticker = new RuptureTicker(100);
        for (int tick = 0; tick < 35; tick++) {
            ticker.tick();
        }

        ticker.refresh();
        assertEquals(RuptureTicker.Step.DAMAGE, ticker.tick());
        assertEquals(36, ticker.totalTicks());

        RuptureTicker.Step finalStep = RuptureTicker.Step.NONE;
        while (ticker.totalTicks() < 100) {
            finalStep = ticker.tick();
        }
        assertEquals(RuptureTicker.Step.EXPIRED, finalStep);
    }
}

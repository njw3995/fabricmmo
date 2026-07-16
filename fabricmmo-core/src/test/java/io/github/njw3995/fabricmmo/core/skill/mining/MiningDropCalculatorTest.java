package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.api.random.RandomSource;
import java.util.ArrayDeque;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MiningDropCalculatorTest {
    @Test
    void motherLodeSuccessProducesTwoBonusCopies() {
        MiningDropCalculator calculator = new MiningDropCalculator(sequence(0.49D));
        MiningDropContext context = context(10000, true, true, false, true);
        assertEquals(MiningDropOutcome.TRIPLE, calculator.roll(context));
    }

    @Test
    void failedMotherLodeFallsBackToNormalDoubleDropRoll() {
        MiningDropCalculator calculator = new MiningDropCalculator(sequence(0.06D, 0.99D));
        MiningDropContext context = context(1000, true, true, false, true);
        assertEquals(MiningDropOutcome.DOUBLE, calculator.roll(context));
    }

    @Test
    void superBreakerConvertsSuccessfulDoubleDropToTripleWhenConfigured() {
        MiningDropCalculator calculator = new MiningDropCalculator(sequence(0.0D));
        MiningDropContext context = new MiningDropContext(
                1000,
                ProgressionMode.RETRO,
                true,
                false,
                true,
                true,
                false);
        assertEquals(MiningDropOutcome.TRIPLE, calculator.roll(context));
    }

    @Test
    void disabledDoubleDropsCannotProduceAnyBonus() {
        MiningDropCalculator calculator = new MiningDropCalculator(sequence());
        MiningDropContext context = new MiningDropContext(
                1000,
                ProgressionMode.RETRO,
                false,
                true,
                true,
                true,
                true);
        assertEquals(MiningDropOutcome.NONE, calculator.roll(context));
    }

    private static MiningDropContext context(int level, boolean doubles, boolean motherLode,
                                             boolean superBreaker, boolean allowTriple) {
        return new MiningDropContext(
                level,
                ProgressionMode.RETRO,
                doubles,
                motherLode,
                superBreaker,
                allowTriple,
                false);
    }

    private static RandomSource sequence(double... values) {
        ArrayDeque<Double> queue = new ArrayDeque<>();
        Arrays.stream(values).forEach(queue::addLast);
        return () -> {
            if (queue.isEmpty()) {
                throw new AssertionError("Unexpected RNG consumption");
            }
            return queue.removeFirst();
        };
    }
}

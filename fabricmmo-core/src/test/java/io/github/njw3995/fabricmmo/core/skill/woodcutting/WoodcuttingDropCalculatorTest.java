package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.function.DoubleSupplier;
import org.junit.jupiter.api.Test;

class WoodcuttingDropCalculatorTest {
    @Test
    void cleanCutsWinsBeforeHarvestLumber() throws Exception {
        WoodcuttingDropCalculator calculator = new WoodcuttingDropCalculator(
                sequence(0.49D), settings());
        assertEquals(WoodcuttingDropOutcome.TRIPLE, calculator.roll(
                new WoodcuttingDropContext(10000, ProgressionMode.RETRO,
                        true, true, false)));
    }

    @Test
    void failedCleanCutsFallsBackToHarvestLumber() throws Exception {
        WoodcuttingDropCalculator calculator = new WoodcuttingDropCalculator(
                sequence(0.99D, 0.0D), settings());
        assertEquals(WoodcuttingDropOutcome.DOUBLE, calculator.roll(
                new WoodcuttingDropContext(1000, ProgressionMode.RETRO,
                        true, true, false)));
    }

    @Test
    void lockedOrDeniedPassivesDoNotRoll() throws Exception {
        WoodcuttingDropCalculator calculator = new WoodcuttingDropCalculator(
                sequence(), settings());
        assertEquals(WoodcuttingDropOutcome.NONE, calculator.roll(
                new WoodcuttingDropContext(0, ProgressionMode.RETRO,
                        false, false, false)));
    }

    @Test
    void probabilityMatchesConfiguredLinearScalingAndLuckyMultiplier() {
        assertEquals(0.50D, WoodcuttingProbability.chance(50, 100, 100.0D, false), 0.000001D);
        assertEquals(0.6665D, WoodcuttingProbability.chance(50, 100, 100.0D, true), 0.000001D);
        assertEquals(0.50D, WoodcuttingProbability.chance(10000, 10000, 50.0D, false), 0.000001D);
    }

    private static WoodcuttingDropSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return WoodcuttingDropSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"));
    }

    private static DoubleSupplier sequence(double... values) {
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

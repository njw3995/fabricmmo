package io.github.njw3995.fabricmmo.core.skill.herbalism;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;

class HerbalismDropCalculatorTest {
    @Test
    void greenTerraTriplesOnlyAfterDoubleDropSucceeds() throws Exception {
        HerbalismSettings settings = settings();
        assertEquals(HerbalismDropOutcome.NONE,
                calculator(settings, 0.999D).roll(50, false, true));
        assertEquals(HerbalismDropOutcome.TRIPLE,
                calculator(settings, 0.0D).roll(50, false, true));
    }

    @Test
    void verdantBountyIsASecondIndependentRoll() throws Exception {
        HerbalismSettings settings = settings();
        assertEquals(HerbalismDropOutcome.TRIPLE,
                calculator(settings, 0.0D, 0.0D).roll(1000, false, false));
        assertEquals(HerbalismDropOutcome.DOUBLE,
                calculator(settings, 0.0D, 0.999D).roll(1000, false, false));
    }

    private static HerbalismDropCalculator calculator(
            HerbalismSettings settings, double... values) {
        ArrayDeque<Double> random = new ArrayDeque<>();
        for (double value : values) {
            random.add(value);
        }
        return new HerbalismDropCalculator(random::removeFirst, settings);
    }

    private static HerbalismSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return HerbalismSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("experience.yml"));
    }
}

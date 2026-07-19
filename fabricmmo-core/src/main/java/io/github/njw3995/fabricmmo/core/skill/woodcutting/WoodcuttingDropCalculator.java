package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import java.util.Objects;
import java.util.function.DoubleSupplier;

/** Deterministic-rollable Clean Cuts then Harvest Lumber resolution. */
public final class WoodcuttingDropCalculator {
    private final DoubleSupplier random;
    private final WoodcuttingDropSettings settings;

    public WoodcuttingDropCalculator(DoubleSupplier random, WoodcuttingDropSettings settings) {
        this.random = Objects.requireNonNull(random, "random");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public WoodcuttingDropOutcome roll(WoodcuttingDropContext context) {
        Objects.requireNonNull(context, "context");
        if (context.cleanCutsAllowed()
                && settings.cleanCutsUnlocked(
                        context.skillLevel(), context.progressionMode())
                && succeeds(WoodcuttingProbability.chance(
                        context.skillLevel(),
                        settings.cleanCutsMaxLevel(context.progressionMode()),
                        settings.cleanCutsChanceMaxPercent(),
                        context.lucky()))) {
            return WoodcuttingDropOutcome.TRIPLE;
        }
        if (context.harvestLumberAllowed()
                && settings.harvestLumberUnlocked(
                        context.skillLevel(), context.progressionMode())
                && succeeds(WoodcuttingProbability.chance(
                        context.skillLevel(),
                        settings.harvestLumberMaxLevel(context.progressionMode()),
                        settings.harvestLumberChanceMaxPercent(),
                        context.lucky()))) {
            return WoodcuttingDropOutcome.DOUBLE;
        }
        return WoodcuttingDropOutcome.NONE;
    }

    private boolean succeeds(double probability) {
        double value = random.getAsDouble();
        if (!Double.isFinite(value) || value < 0.0D || value >= 1.0D) {
            throw new IllegalStateException("Random source must return a value in [0,1)");
        }
        return value < probability;
    }
}

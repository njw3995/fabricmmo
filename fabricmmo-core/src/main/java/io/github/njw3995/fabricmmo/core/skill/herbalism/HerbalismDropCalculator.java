package io.github.njw3995.fabricmmo.core.skill.herbalism;

import java.util.Objects;
import java.util.function.DoubleSupplier;

/** Deterministic Double Drops, Verdant Bounty, and Green Terra drop resolution. */
public final class HerbalismDropCalculator {
    private final DoubleSupplier random;
    private final HerbalismSettings settings;

    public HerbalismDropCalculator(DoubleSupplier random, HerbalismSettings settings) {
        this.random = Objects.requireNonNull(random, "random");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public HerbalismDropOutcome roll(int level, boolean lucky, boolean greenTerra) {
        if (level < settings.doubleDropsUnlockLevel()
                || !HerbalismProbability.roll(random.getAsDouble(), settings.doubleDropsChance(level, lucky))) {
            return HerbalismDropOutcome.NONE;
        }
        if (greenTerra) {
            return HerbalismDropOutcome.TRIPLE;
        }
        if (level >= settings.verdantBountyUnlockLevel()
                && HerbalismProbability.roll(random.getAsDouble(), settings.verdantBountyChance(level, lucky))) {
            return HerbalismDropOutcome.TRIPLE;
        }
        return HerbalismDropOutcome.DOUBLE;
    }
}

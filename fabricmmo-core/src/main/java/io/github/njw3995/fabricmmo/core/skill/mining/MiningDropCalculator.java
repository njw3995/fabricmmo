package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.random.RandomSource;
import java.util.Objects;

public final class MiningDropCalculator {
    private final RandomSource random;
    private final MiningDropSettings settings;

    public MiningDropCalculator(RandomSource random) {
        this(random, MiningDropSettings.upstreamDefaults());
    }

    public MiningDropCalculator(RandomSource random, MiningDropSettings settings) {
        this.random = Objects.requireNonNull(random, "random");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public MiningDropOutcome roll(MiningDropContext context) {
        if (!context.doubleDropsEnabled()) {
            return MiningDropOutcome.NONE;
        }

        if (context.motherLodeEnabled() && random.roll(motherLodeChance(context))) {
            return MiningDropOutcome.TRIPLE;
        }

        if (!random.roll(doubleDropsChance(context))) {
            return MiningDropOutcome.NONE;
        }

        return context.superBreakerActive() && context.allowSuperBreakerTripleDrops()
                ? MiningDropOutcome.TRIPLE
                : MiningDropOutcome.DOUBLE;
    }

    public double doubleDropsChance(MiningDropContext context) {
        return MiningProbability.chance(
                context.skillLevel(),
                settings.doubleDropsMaxLevel(context.progressionMode()),
                settings.doubleDropsChanceMaxPercent(),
                context.lucky());
    }

    public double motherLodeChance(MiningDropContext context) {
        return MiningProbability.chance(
                context.skillLevel(),
                settings.motherLodeMaxLevel(context.progressionMode()),
                settings.motherLodeChanceMaxPercent(),
                context.lucky());
    }
}

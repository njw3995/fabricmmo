package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.api.random.RandomSource;
import java.util.Objects;

public final class MiningDropCalculator {
    public static final double DOUBLE_DROPS_CHANCE_MAX_PERCENT = 100.0D;
    public static final int DOUBLE_DROPS_MAX_LEVEL_STANDARD = 100;
    public static final int DOUBLE_DROPS_MAX_LEVEL_RETRO = 1000;
    public static final double MOTHER_LODE_CHANCE_MAX_PERCENT = 50.0D;
    public static final int MOTHER_LODE_MAX_LEVEL_STANDARD = 1000;
    public static final int MOTHER_LODE_MAX_LEVEL_RETRO = 10000;

    private final RandomSource random;

    public MiningDropCalculator(RandomSource random) {
        this.random = Objects.requireNonNull(random, "random");
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

    public static double doubleDropsChance(MiningDropContext context) {
        return MiningProbability.chance(
                context.skillLevel(),
                modeValue(context.progressionMode(),
                        DOUBLE_DROPS_MAX_LEVEL_STANDARD,
                        DOUBLE_DROPS_MAX_LEVEL_RETRO),
                DOUBLE_DROPS_CHANCE_MAX_PERCENT,
                context.lucky());
    }

    public static double motherLodeChance(MiningDropContext context) {
        return MiningProbability.chance(
                context.skillLevel(),
                modeValue(context.progressionMode(),
                        MOTHER_LODE_MAX_LEVEL_STANDARD,
                        MOTHER_LODE_MAX_LEVEL_RETRO),
                MOTHER_LODE_CHANCE_MAX_PERCENT,
                context.lucky());
    }

    private static int modeValue(ProgressionMode mode, int standard, int retro) {
        return mode == ProgressionMode.RETRO ? retro : standard;
    }
}

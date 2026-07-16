package io.github.njw3995.fabricmmo.core.ability;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.time.Duration;
import java.util.Objects;

/**
 * Calculates the base duration of an mcMMO super ability before activation perks are applied.
 *
 * <p>The defaults are pinned to mcMMO 2.3.000 {@code advanced.yml} and the integer-division
 * behavior in {@code McMMOPlayer#checkAbilityActivation}.</p>
 */
public final class AbilityDurationFormula {
    public static final int BASE_SECONDS = 2;
    public static final int STANDARD_CAP_LEVEL = 100;
    public static final int STANDARD_INCREASE_LEVEL = 5;
    public static final int RETRO_CAP_LEVEL = 1000;
    public static final int RETRO_INCREASE_LEVEL = 50;

    private AbilityDurationFormula() {
    }

    public static Duration baseDuration(int skillLevel, ProgressionMode mode, int maximumSeconds) {
        Objects.requireNonNull(mode, "mode");
        return switch (mode) {
            case STANDARD -> baseDuration(
                    skillLevel,
                    STANDARD_CAP_LEVEL,
                    STANDARD_INCREASE_LEVEL,
                    maximumSeconds);
            case RETRO -> baseDuration(
                    skillLevel,
                    RETRO_CAP_LEVEL,
                    RETRO_INCREASE_LEVEL,
                    maximumSeconds);
        };
    }

    public static Duration baseDuration(
            int skillLevel,
            int capLevel,
            int increaseLevel,
            int maximumSeconds) {
        if (skillLevel < 0) {
            throw new IllegalArgumentException("skillLevel must be non-negative");
        }
        if (increaseLevel < 1) {
            throw new IllegalArgumentException("increaseLevel must be positive");
        }
        if (maximumSeconds < 0) {
            throw new IllegalArgumentException("maximumSeconds must be non-negative");
        }

        int effectiveLevel = capLevel > 0 ? Math.min(capLevel, skillLevel) : skillLevel;
        int seconds = BASE_SECONDS + (effectiveLevel / increaseLevel);
        if (maximumSeconds > 0) {
            seconds = Math.min(seconds, maximumSeconds);
        }
        return Duration.ofSeconds(seconds);
    }
}

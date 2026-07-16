package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.util.Objects;

public record MiningDropContext(
        int skillLevel,
        ProgressionMode progressionMode,
        boolean doubleDropsEnabled,
        boolean motherLodeEnabled,
        boolean superBreakerActive,
        boolean allowSuperBreakerTripleDrops,
        boolean lucky) {

    public MiningDropContext {
        if (skillLevel < 0) {
            throw new IllegalArgumentException("skillLevel must not be negative");
        }
        Objects.requireNonNull(progressionMode, "progressionMode");
    }
}

package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.util.Objects;

/** Pure inputs for Harvest Lumber and Clean Cuts. */
public record WoodcuttingDropContext(
        int skillLevel,
        ProgressionMode progressionMode,
        boolean harvestLumberAllowed,
        boolean cleanCutsAllowed,
        boolean lucky) {
    public WoodcuttingDropContext {
        if (skillLevel < 0) {
            throw new IllegalArgumentException("skillLevel must not be negative");
        }
        Objects.requireNonNull(progressionMode, "progressionMode");
    }
}

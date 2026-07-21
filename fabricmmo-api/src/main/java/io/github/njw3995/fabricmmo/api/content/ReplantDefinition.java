package io.github.njw3995.fabricmmo.api.content;

import java.util.List;
import java.util.Objects;

/** Declarative Green Thumb/Green Terra replant behavior for an ageable crop. */
public record ReplantDefinition(
        ContentSelector plantingItem,
        String ageProperty,
        List<Integer> rankAges,
        int activeAbilityRankBonus,
        int delayTicks) {
    public ReplantDefinition {
        Objects.requireNonNull(plantingItem, "plantingItem");
        ageProperty = Objects.requireNonNull(ageProperty, "ageProperty").trim();
        if (ageProperty.isEmpty()) {
            throw new IllegalArgumentException("ageProperty must not be blank");
        }
        rankAges = List.copyOf(Objects.requireNonNull(rankAges, "rankAges"));
        if (rankAges.isEmpty()) {
            throw new IllegalArgumentException("rankAges must contain at least one stage");
        }
        if (rankAges.stream().anyMatch(value -> value == null || value < 0)) {
            throw new IllegalArgumentException("rankAges must be non-negative");
        }
        if (activeAbilityRankBonus < 0 || delayTicks < 0) {
            throw new IllegalArgumentException(
                    "activeAbilityRankBonus and delayTicks must be non-negative");
        }
    }
}

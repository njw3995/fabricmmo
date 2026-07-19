package io.github.njw3995.fabricmmo.core.skill.excavation;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.util.Objects;
import java.util.Set;

/** One configured Archaeology treasure entry from treasures.yml. */
public record ExcavationTreasure(
        String key,
        NamespacedId itemId,
        int amount,
        int xp,
        double dropChancePercent,
        int standardLevel,
        int retroLevel,
        Set<NamespacedId> dropsFrom) {

    public ExcavationTreasure {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(itemId, "itemId");
        dropsFrom = Set.copyOf(dropsFrom);
        if (amount <= 0 || xp < 0 || dropChancePercent < 0.0D
                || standardLevel < 0 || retroLevel < 0) {
            throw new IllegalArgumentException("Invalid excavation treasure " + key);
        }
    }

    public int requiredLevel(ProgressionMode mode) {
        return mode == ProgressionMode.RETRO ? retroLevel : standardLevel;
    }
}

package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.util.Objects;

/** One configured Hylian Luck treasure entry. */
public record HerbalismHylianTreasure(
        String key,
        NamespacedId itemId,
        int amount,
        int xp,
        double dropChancePercent,
        int standardLevel,
        int retroLevel) {

    public HerbalismHylianTreasure {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(itemId, "itemId");
        if (amount <= 0 || xp < 0 || dropChancePercent < 0.0D
                || standardLevel < 0 || retroLevel < 0) {
            throw new IllegalArgumentException("Invalid Hylian treasure " + key);
        }
    }

    public int requiredLevel(ProgressionMode mode) {
        return mode == ProgressionMode.RETRO ? retroLevel : standardLevel;
    }
}

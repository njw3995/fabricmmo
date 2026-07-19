package io.github.njw3995.fabricmmo.core.skill.fishing;

import java.util.Locale;

/** Upstream fishing treasure rarity ordering. */
public enum FishingRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    MYTHIC;

    public static FishingRarity parse(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}

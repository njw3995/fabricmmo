package io.github.njw3995.fabricmmo.core.party;

import java.util.Locale;

/** Upstream item-share categories. */
public enum ItemShareCategory {
    LOOT,
    MINING,
    HERBALISM,
    WOODCUTTING,
    MISC;

    public static ItemShareCategory parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown item-share category: " + value, exception);
        }
    }
}

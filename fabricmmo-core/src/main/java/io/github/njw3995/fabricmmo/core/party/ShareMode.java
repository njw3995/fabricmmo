package io.github.njw3995.fabricmmo.core.party;

import java.util.Locale;

/** Upstream mcMMO party share modes. XP sharing supports NONE/EQUAL; item sharing supports all. */
public enum ShareMode {
    NONE,
    EQUAL,
    RANDOM;

    public static ShareMode parse(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "none", "off", "false", "disable", "disabled" -> NONE;
            case "equal", "even", "on", "true", "enable", "enabled" -> EQUAL;
            case "random" -> RANDOM;
            default -> throw new IllegalArgumentException("Unknown share mode: " + value);
        };
    }
}

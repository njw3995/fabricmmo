package io.github.njw3995.fabricmmo.api.content;

import java.util.Objects;

/** A loader-independent maturity check evaluated from a block state's integer property. */
public record MaturityRequirement(Mode mode, String property, int value) {
    public enum Mode {
        ANY,
        INTEGER_PROPERTY_MAXIMUM,
        INTEGER_PROPERTY_AT_LEAST
    }

    public MaturityRequirement {
        Objects.requireNonNull(mode, "mode");
        property = Objects.requireNonNull(property, "property").trim();
        if (mode == Mode.ANY) {
            property = "";
            value = 0;
        } else if (property.isEmpty()) {
            throw new IllegalArgumentException("A maturity property is required for " + mode);
        }
        if (value < 0) {
            throw new IllegalArgumentException("Maturity value must be non-negative");
        }
    }

    public static MaturityRequirement any() {
        return new MaturityRequirement(Mode.ANY, "", 0);
    }

    public static MaturityRequirement maximum(String property) {
        return new MaturityRequirement(Mode.INTEGER_PROPERTY_MAXIMUM, property, 0);
    }

    public static MaturityRequirement atLeast(String property, int value) {
        return new MaturityRequirement(Mode.INTEGER_PROPERTY_AT_LEAST, property, value);
    }
}

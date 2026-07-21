package io.github.njw3995.fabricmmo.core.skill.axes;

/** Persisted Skull Splitter cooldown timestamp. */
public record AxesAbilityData(long skullSplitterLastUsed) {
    public static final AxesAbilityData EMPTY = new AxesAbilityData(0L);

    public AxesAbilityData {
        if (skullSplitterLastUsed < 0L) {
            throw new IllegalArgumentException("Cooldown timestamp must not be negative");
        }
    }
}

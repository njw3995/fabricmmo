package io.github.njw3995.fabricmmo.core.skill.excavation;

/** Persisted Giga Drill Breaker cooldown timestamp. */
public record ExcavationAbilityData(long gigaDrillLastUsed) {
    public static final ExcavationAbilityData EMPTY = new ExcavationAbilityData(0L);

    public ExcavationAbilityData {
        if (gigaDrillLastUsed < 0L) {
            throw new IllegalArgumentException("Cooldown timestamp must not be negative");
        }
    }
}

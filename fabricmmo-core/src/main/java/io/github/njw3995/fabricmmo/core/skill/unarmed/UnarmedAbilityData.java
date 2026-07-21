package io.github.njw3995.fabricmmo.core.skill.unarmed;

/** Persisted Berserk cooldown timestamp. */
public record UnarmedAbilityData(long berserkLastUsed) {
    public static final UnarmedAbilityData EMPTY = new UnarmedAbilityData(0L);

    public UnarmedAbilityData {
        if (berserkLastUsed < 0L) {
            throw new IllegalArgumentException("Cooldown timestamp must not be negative");
        }
    }
}

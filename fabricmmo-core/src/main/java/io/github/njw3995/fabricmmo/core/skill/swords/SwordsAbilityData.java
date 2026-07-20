package io.github.njw3995.fabricmmo.core.skill.swords;

/** Persisted Serrated Strikes cooldown timestamp. */
public record SwordsAbilityData(long serratedStrikesLastUsed) {
    public static final SwordsAbilityData EMPTY = new SwordsAbilityData(0L);

    public SwordsAbilityData {
        if (serratedStrikesLastUsed < 0L) {
            throw new IllegalArgumentException("Cooldown timestamp must not be negative");
        }
    }
}

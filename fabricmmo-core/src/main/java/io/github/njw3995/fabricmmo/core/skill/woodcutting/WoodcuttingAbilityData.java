package io.github.njw3995.fabricmmo.core.skill.woodcutting;

/** Persisted Tree Feller cooldown timestamp. */
public record WoodcuttingAbilityData(long treeFellerLastUsed) {
    public static final WoodcuttingAbilityData EMPTY = new WoodcuttingAbilityData(0L);

    public WoodcuttingAbilityData {
        if (treeFellerLastUsed < 0L) {
            throw new IllegalArgumentException("Cooldown timestamp must not be negative");
        }
    }
}

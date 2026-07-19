package io.github.njw3995.fabricmmo.core.skill.herbalism;

/** Persisted Green Terra cooldown timestamp. */
public record HerbalismAbilityData(long greenTerraLastUsed) {
    public static final HerbalismAbilityData EMPTY = new HerbalismAbilityData(0L);

    public HerbalismAbilityData {
        if (greenTerraLastUsed < 0L) {
            throw new IllegalArgumentException("Cooldown timestamp must not be negative");
        }
    }
}

package io.github.njw3995.fabricmmo.api.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.Objects;

/**
 * Declares static entity XP for core-owned combat or animal-taming mechanics.
 *
 * <p>For {@link Scope#COMBAT}, {@code xp} is the base amount multiplied by actual health damage
 * and the existing mcMMO origin multiplier. For {@link Scope#TAMING}, it is the one-time XP value
 * awarded by the normal server-side tame event.</p>
 */
public record EntityXpContentDefinition(
        NamespacedId id,
        Scope scope,
        ContentSelector entity,
        double xp,
        Map<String, String> metadata) {
    public enum Scope {
        COMBAT,
        TAMING
    }

    public EntityXpContentDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(entity, "entity");
        if (!Double.isFinite(xp) || xp < 0.0D) {
            throw new IllegalArgumentException("xp must be finite and non-negative");
        }
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }

    public static EntityXpContentDefinition of(
            NamespacedId id,
            Scope scope,
            ContentSelector entity,
            double xp) {
        return new EntityXpContentDefinition(id, scope, entity, xp, Map.of());
    }
}

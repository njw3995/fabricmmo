package io.github.njw3995.fabricmmo.api.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.Objects;

/**
 * Declares an external brewing transformation that should participate in core Alchemy progression.
 * The external mod remains responsible for performing the recipe; FabricMMO only validates the
 * completed server-side inventory transformation and awards configured Alchemy stage XP.
 */
public record BrewingContentDefinition(
        NamespacedId id,
        ContentSelector ingredient,
        ContentSelector input,
        ContentSelector output,
        int stage,
        Map<String, String> metadata) {
    public BrewingContentDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ingredient, "ingredient");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        if (stage < 1 || stage > 5) {
            throw new IllegalArgumentException("Alchemy stage must be between 1 and 5");
        }
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }
    public static Builder builder(
            NamespacedId id,
            ContentSelector ingredient,
            ContentSelector input,
            ContentSelector output,
            int stage) {
        return new Builder(id, ingredient, input, output, stage);
    }

    public static final class Builder {
        private final NamespacedId id;
        private final ContentSelector ingredient;
        private final ContentSelector input;
        private final ContentSelector output;
        private final int stage;
        private Map<String, String> metadata = Map.of();

        private Builder(
                NamespacedId id,
                ContentSelector ingredient,
                ContentSelector input,
                ContentSelector output,
                int stage) {
            this.id = Objects.requireNonNull(id, "id");
            this.ingredient = Objects.requireNonNull(ingredient, "ingredient");
            this.input = Objects.requireNonNull(input, "input");
            this.output = Objects.requireNonNull(output, "output");
            this.stage = stage;
        }

        public Builder metadata(Map<String, String> value) {
            this.metadata = Map.copyOf(Objects.requireNonNull(value, "value"));
            return this;
        }

        public BrewingContentDefinition build() {
            return new BrewingContentDefinition(id, ingredient, input, output, stage, metadata);
        }
    }

}

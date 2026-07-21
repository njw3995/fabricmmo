package io.github.njw3995.fabricmmo.api.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Declares modded gathering content while leaving progression and gameplay decisions to core.
 */
public record GatheringContentDefinition(
        NamespacedId id,
        NamespacedId skillId,
        ContentSelector block,
        int xp,
        Set<ContentSelector> validTools,
        boolean naturalBlocksOnly,
        MaturityRequirement maturity,
        boolean bonusDrops,
        boolean activeAbility,
        Optional<ReplantDefinition> replant,
        Map<String, String> metadata) {
    public GatheringContentDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(block, "block");
        if (xp < 0) {
            throw new IllegalArgumentException("xp must be non-negative");
        }
        validTools = Set.copyOf(new TreeSet<>(Objects.requireNonNull(validTools, "validTools")));
        Objects.requireNonNull(maturity, "maturity");
        replant = Objects.requireNonNull(replant, "replant");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
        if (replant.isPresent() && maturity.mode() == MaturityRequirement.Mode.ANY) {
            throw new IllegalArgumentException("Replantable content must define a maturity rule");
        }
    }
    public static Builder builder(
            NamespacedId id,
            NamespacedId skillId,
            ContentSelector block,
            int xp) {
        return new Builder(id, skillId, block, xp);
    }

    /** Fluent construction with conservative defaults for external content. */
    public static final class Builder {
        private final NamespacedId id;
        private final NamespacedId skillId;
        private final ContentSelector block;
        private final int xp;
        private Set<ContentSelector> validTools = Set.of();
        private boolean naturalBlocksOnly = true;
        private MaturityRequirement maturity = MaturityRequirement.any();
        private boolean bonusDrops;
        private boolean activeAbility;
        private Optional<ReplantDefinition> replant = Optional.empty();
        private Map<String, String> metadata = Map.of();

        private Builder(
                NamespacedId id,
                NamespacedId skillId,
                ContentSelector block,
                int xp) {
            this.id = Objects.requireNonNull(id, "id");
            this.skillId = Objects.requireNonNull(skillId, "skillId");
            this.block = Objects.requireNonNull(block, "block");
            this.xp = xp;
        }

        public Builder validTools(ContentSelector... selectors) {
            this.validTools = Set.of(selectors);
            return this;
        }

        public Builder validTools(Set<ContentSelector> selectors) {
            this.validTools = Set.copyOf(Objects.requireNonNull(selectors, "selectors"));
            return this;
        }

        public Builder naturalBlocksOnly(boolean value) {
            this.naturalBlocksOnly = value;
            return this;
        }

        public Builder maturity(MaturityRequirement value) {
            this.maturity = Objects.requireNonNull(value, "value");
            return this;
        }

        public Builder bonusDrops(boolean value) {
            this.bonusDrops = value;
            return this;
        }

        public Builder activeAbility(boolean value) {
            this.activeAbility = value;
            return this;
        }

        public Builder replant(ReplantDefinition value) {
            this.replant = Optional.of(Objects.requireNonNull(value, "value"));
            return this;
        }

        public Builder metadata(Map<String, String> value) {
            this.metadata = Map.copyOf(Objects.requireNonNull(value, "value"));
            return this;
        }

        public GatheringContentDefinition build() {
            return new GatheringContentDefinition(
                    id,
                    skillId,
                    block,
                    xp,
                    validTools,
                    naturalBlocksOnly,
                    maturity,
                    bonusDrops,
                    activeAbility,
                    replant,
                    metadata);
        }
    }

}

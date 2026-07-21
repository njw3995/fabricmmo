package io.github.njw3995.fabricmmo.api.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.Optional;

public interface GatheringContentRegistryView {
    Optional<GatheringContentDefinition> find(NamespacedId id);

    List<GatheringContentDefinition> definitions();

    List<GatheringContentDefinition> definitionsForSkill(NamespacedId skillId);

    boolean frozen();

    static GatheringContentRegistryView unsupported() {
        return UnsupportedView.INSTANCE;
    }

    enum UnsupportedView implements GatheringContentRegistryView {
        INSTANCE;

        @Override
        public Optional<GatheringContentDefinition> find(NamespacedId id) {
            return Optional.empty();
        }

        @Override
        public List<GatheringContentDefinition> definitions() {
            return List.of();
        }

        @Override
        public List<GatheringContentDefinition> definitionsForSkill(NamespacedId skillId) {
            return List.of();
        }

        @Override
        public boolean frozen() {
            return true;
        }
    }
}

package io.github.njw3995.fabricmmo.api.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.Optional;

public interface BrewingContentRegistryView {
    Optional<BrewingContentDefinition> find(NamespacedId id);

    List<BrewingContentDefinition> definitions();

    boolean frozen();

    static BrewingContentRegistryView unsupported() {
        return UnsupportedView.INSTANCE;
    }

    enum UnsupportedView implements BrewingContentRegistryView {
        INSTANCE;

        @Override
        public Optional<BrewingContentDefinition> find(NamespacedId id) {
            return Optional.empty();
        }

        @Override
        public List<BrewingContentDefinition> definitions() {
            return List.of();
        }

        @Override
        public boolean frozen() {
            return true;
        }
    }
}

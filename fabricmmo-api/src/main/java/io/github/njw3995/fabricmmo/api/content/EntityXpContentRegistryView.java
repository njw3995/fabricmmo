package io.github.njw3995.fabricmmo.api.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.Optional;

public interface EntityXpContentRegistryView {
    Optional<EntityXpContentDefinition> find(NamespacedId id);

    List<EntityXpContentDefinition> definitions();

    List<EntityXpContentDefinition> definitionsForScope(EntityXpContentDefinition.Scope scope);

    boolean frozen();

    static EntityXpContentRegistryView unsupported() {
        return Unsupported.INSTANCE;
    }

    enum Unsupported implements EntityXpContentRegistryView {
        INSTANCE;

        @Override
        public Optional<EntityXpContentDefinition> find(NamespacedId id) {
            return Optional.empty();
        }

        @Override
        public List<EntityXpContentDefinition> definitions() {
            return List.of();
        }

        @Override
        public List<EntityXpContentDefinition> definitionsForScope(
                EntityXpContentDefinition.Scope scope) {
            return List.of();
        }

        @Override
        public boolean frozen() {
            return true;
        }
    }
}

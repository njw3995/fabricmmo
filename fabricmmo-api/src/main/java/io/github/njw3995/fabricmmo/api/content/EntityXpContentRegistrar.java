package io.github.njw3995.fabricmmo.api.content;

@FunctionalInterface
public interface EntityXpContentRegistrar {
    void registerEntityXpContent(EntityXpContentDefinition definition);

    static EntityXpContentRegistrar unsupported() {
        return definition -> {
            throw new UnsupportedOperationException(
                    "This FabricMMO API implementation does not support entity XP content registration");
        };
    }
}

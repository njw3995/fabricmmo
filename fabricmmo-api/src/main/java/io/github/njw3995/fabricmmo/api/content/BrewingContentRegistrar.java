package io.github.njw3995.fabricmmo.api.content;

@FunctionalInterface
public interface BrewingContentRegistrar {
    void registerBrewingContent(BrewingContentDefinition definition);

    static BrewingContentRegistrar unsupported() {
        return definition -> {
            throw new UnsupportedOperationException(
                    "This FabricMMO API implementation does not support brewing content registration");
        };
    }
}

package io.github.njw3995.fabricmmo.api.content;

@FunctionalInterface
public interface GatheringContentRegistrar {
    void registerGatheringContent(GatheringContentDefinition definition);

    static GatheringContentRegistrar unsupported() {
        return definition -> {
            throw new UnsupportedOperationException(
                    "This FabricMMO API implementation does not support gathering content registration");
        };
    }
}

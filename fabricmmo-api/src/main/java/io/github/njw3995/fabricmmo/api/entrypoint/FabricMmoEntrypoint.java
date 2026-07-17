package io.github.njw3995.fabricmmo.api.entrypoint;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;

/**
 * Fabric Loader entrypoint used by addons to register with FabricMMO before registries are frozen.
 */
@FunctionalInterface
public interface FabricMmoEntrypoint {
    String KEY = "fabricmmo";

    void register(FabricMmoApi api);
}

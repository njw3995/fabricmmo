package io.github.njw3995.fabricmmo.core.runtime;

import io.github.njw3995.fabricmmo.core.bootstrap.DefaultFabricMmoApi;
import io.github.njw3995.fabricmmo.core.bootstrap.FabricMmoBootstrap;
import io.github.njw3995.fabricmmo.core.persistence.ProgressionStore;
import io.github.njw3995.fabricmmo.core.persistence.PropertiesProgressionStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Owns the API and persistence resources for one logical Minecraft server instance.
 */
public final class FabricMmoServerRuntime implements AutoCloseable {
    private final ProgressionStore store;
    private final DefaultFabricMmoApi api;
    private boolean closed;

    private FabricMmoServerRuntime(ProgressionStore store, DefaultFabricMmoApi api) {
        this.store = store;
        this.api = api;
    }

    public static FabricMmoServerRuntime start(
            Path playerDataDirectory,
            Consumer<DefaultFabricMmoApi> addonRegistration) throws IOException {
        Objects.requireNonNull(playerDataDirectory, "playerDataDirectory");
        Objects.requireNonNull(addonRegistration, "addonRegistration");

        ProgressionStore store = new PropertiesProgressionStore(playerDataDirectory);
        try {
            DefaultFabricMmoApi api = FabricMmoBootstrap.create(store, addonRegistration);
            return new FabricMmoServerRuntime(store, api);
        } catch (RuntimeException | Error exception) {
            try {
                store.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    public synchronized DefaultFabricMmoApi api() {
        if (closed) {
            throw new IllegalStateException("FabricMMO server runtime is closed");
        }
        return api;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        store.close();
    }
}

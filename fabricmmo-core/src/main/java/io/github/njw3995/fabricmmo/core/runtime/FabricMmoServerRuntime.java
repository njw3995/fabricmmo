package io.github.njw3995.fabricmmo.core.runtime;

import io.github.njw3995.fabricmmo.core.bootstrap.DefaultFabricMmoApi;
import io.github.njw3995.fabricmmo.core.bootstrap.FabricMmoBootstrap;
import io.github.njw3995.fabricmmo.core.persistence.ManagedProgressionStore;
import io.github.njw3995.fabricmmo.core.persistence.MySqlSettings;
import io.github.njw3995.fabricmmo.core.persistence.ProgressionStoreFactory;
import io.github.njw3995.fabricmmo.core.progression.ProgressionSettings;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

/** Owns the API and persistence resources for one logical Minecraft server instance. */
public final class FabricMmoServerRuntime implements AutoCloseable {
    private final ManagedProgressionStore store;
    private final DefaultFabricMmoApi api;
    private boolean closed;

    private FabricMmoServerRuntime(ManagedProgressionStore store, DefaultFabricMmoApi api) {
        this.store = store;
        this.api = api;
    }

    public static FabricMmoServerRuntime start(
            Path playerDataDirectory,
            Consumer<DefaultFabricMmoApi> addonRegistration) throws IOException {
        return start(playerDataDirectory, ProgressionSettings.upstreamDefaults(), addonRegistration);
    }

    public static FabricMmoServerRuntime start(
            Path playerDataDirectory,
            ProgressionSettings progressionSettings,
            Consumer<DefaultFabricMmoApi> addonRegistration) throws IOException {
        return start(playerDataDirectory, progressionSettings,
                new MySqlSettings(false, false, "localhost", 3306, "DataBaseName",
                        "UserName", "UserPassword", "mcmmo_", true, true, 20),
                addonRegistration);
    }

    public static FabricMmoServerRuntime start(
            Path playerDataDirectory,
            ProgressionSettings progressionSettings,
            MySqlSettings mysqlSettings,
            Consumer<DefaultFabricMmoApi> addonRegistration) throws IOException {
        Objects.requireNonNull(playerDataDirectory, "playerDataDirectory");
        Objects.requireNonNull(progressionSettings, "progressionSettings");
        Objects.requireNonNull(mysqlSettings, "mysqlSettings");
        Objects.requireNonNull(addonRegistration, "addonRegistration");

        ManagedProgressionStore store = ProgressionStoreFactory.open(playerDataDirectory, mysqlSettings);
        try {
            DefaultFabricMmoApi api = FabricMmoBootstrap.create(
                    store,
                    new io.github.njw3995.fabricmmo.core.protection.AllowAllProtectionService(),
                    java.time.Clock.systemUTC(),
                    progressionSettings,
                    addonRegistration);
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
        if (closed) throw new IllegalStateException("FabricMMO server runtime is closed");
        return api;
    }

    public synchronized ManagedProgressionStore store() {
        if (closed) throw new IllegalStateException("FabricMMO server runtime is closed");
        return store;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;
        IOException failure = null;
        try {
            api.markers().close();
        } catch (IOException exception) {
            failure = exception;
        }
        try {
            store.close();
        } catch (IOException exception) {
            if (failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }
        if (failure != null) throw failure;
    }
}

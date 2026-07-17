package io.github.njw3995.fabricmmo.core.fabric;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.entrypoint.FabricMmoEntrypoint;
import io.github.njw3995.fabricmmo.core.runtime.FabricMmoServerRuntime;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FabricMmoFabricRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO");
    private static FabricMmoServerRuntime runtime;

    private FabricMmoFabricRuntime() {
    }

    public static synchronized void start(MinecraftServer server) {
        if (runtime != null) {
            throw new IllegalStateException("FabricMMO server runtime is already active");
        }

        Path playerDataDirectory = resolvePlayerDataDirectory(server.getSavePath(WorldSavePath.ROOT));
        try {
            runtime = FabricMmoServerRuntime.start(playerDataDirectory, api ->
                    FabricLoader.getInstance().invokeEntrypoints(
                            FabricMmoEntrypoint.KEY,
                            FabricMmoEntrypoint.class,
                            entrypoint -> entrypoint.register(api)));
            LOGGER.info("Started with {} registered skills; player data directory: {}",
                    runtime.api().skillRegistry().skills().size(), playerDataDirectory);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to start FabricMMO persistence", exception);
        }
    }

    static Path resolvePlayerDataDirectory(Path worldRoot) {
        Objects.requireNonNull(worldRoot, "worldRoot");
        return worldRoot.resolve("fabricmmo")
                .resolve("players")
                .toAbsolutePath()
                .normalize();
    }

    public static synchronized FabricMmoApi requireApi() {
        if (runtime == null) {
            throw new IllegalStateException("FabricMMO server runtime is not active");
        }
        return runtime.api();
    }

    public static synchronized boolean running() {
        return runtime != null;
    }

    public static synchronized void stop() {
        if (runtime == null) {
            return;
        }
        FabricMmoServerRuntime active = runtime;
        runtime = null;
        try {
            active.close();
            LOGGER.info("Stopped FabricMMO server runtime");
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to stop FabricMMO persistence", exception);
        }
    }
}

package io.github.njw3995.fabricmmo.core.fabric;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.entrypoint.FabricMmoEntrypoint;
import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.block.ChunkPlacedBlockTracker;
import io.github.njw3995.fabricmmo.core.block.PlacedBlockTracker;
import io.github.njw3995.fabricmmo.core.runtime.FabricMmoServerRuntime;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningDropSettings;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningXpTable;
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
    private static MiningXpTable miningXpTable;
    private static MiningDropSettings miningDropSettings;
    private static PlacedBlockTracker placedBlockTracker;

    private FabricMmoFabricRuntime() {
    }

    public static synchronized void start(MinecraftServer server) {
        if (runtime != null) {
            throw new IllegalStateException("FabricMMO server runtime is already active");
        }

        Path worldRoot = server.getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
        Path playerDataDirectory = resolvePlayerDataDirectory(worldRoot);
        Path placedBlockDirectory = resolvePlacedBlockDirectory(worldRoot);
        Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve("fabricmmo");
        Path experienceFile = configDirectory.resolve("experience.yml");
        Path configFile = configDirectory.resolve("config.yml");
        Path advancedFile = configDirectory.resolve("advanced.yml");
        Path skillRanksFile = configDirectory.resolve("skillranks.yml");

        PlacedBlockTracker newTracker = null;
        try {
            MiningXpTable newXpTable = MiningXpTable.loadConfigured(experienceFile);
            MiningDropSettings newDropSettings = MiningDropSettings.load(
                    configFile, advancedFile, skillRanksFile);
            newTracker = new ChunkPlacedBlockTracker(placedBlockDirectory);
            FabricMmoServerRuntime newRuntime = FabricMmoServerRuntime.start(
                    playerDataDirectory,
                    api -> FabricLoader.getInstance().invokeEntrypoints(
                            FabricMmoEntrypoint.KEY,
                            FabricMmoEntrypoint.class,
                            entrypoint -> entrypoint.register(api)));
            runtime = newRuntime;
            miningXpTable = newXpTable;
            miningDropSettings = newDropSettings;
            placedBlockTracker = newTracker;
            LOGGER.info(
                    "Started with {} registered skills; player data directory: {}; placed-block directory: {}",
                    runtime.api().skillRegistry().skills().size(),
                    playerDataDirectory,
                    placedBlockDirectory);
        } catch (IOException exception) {
            closeAfterFailedStart(newTracker, exception);
            throw new UncheckedIOException("Unable to start FabricMMO persistence", exception);
        } catch (RuntimeException | Error exception) {
            closeAfterFailedStart(newTracker, exception);
            throw exception;
        }
    }

    static Path resolvePlayerDataDirectory(Path worldRoot) {
        Objects.requireNonNull(worldRoot, "worldRoot");
        return worldRoot.resolve("fabricmmo")
                .resolve("players")
                .toAbsolutePath()
                .normalize();
    }

    static Path resolvePlacedBlockDirectory(Path worldRoot) {
        Objects.requireNonNull(worldRoot, "worldRoot");
        return worldRoot.resolve("fabricmmo")
                .resolve("placed-blocks")
                .toAbsolutePath()
                .normalize();
    }

    public static synchronized FabricMmoApi requireApi() {
        if (runtime == null) {
            throw new IllegalStateException("FabricMMO server runtime is not active");
        }
        return runtime.api();
    }

    public static synchronized MiningDropSettings miningDropSettings() {
        if (miningDropSettings == null) {
            throw new IllegalStateException("FabricMMO Mining drop configuration is not active");
        }
        return miningDropSettings;
    }

    public static synchronized int miningXpFor(NamespacedId blockId) {
        if (miningXpTable == null) {
            throw new IllegalStateException("FabricMMO Mining configuration is not active");
        }
        return miningXpTable.xpFor(blockId);
    }

    public static synchronized boolean isPlayerPlaced(BlockLocation location) {
        return withPlacedBlockTracker(tracker -> tracker.isPlaced(location));
    }

    public static synchronized void markPlayerPlaced(BlockLocation location) {
        withPlacedBlockTracker(tracker -> {
            tracker.markPlaced(location);
            return false;
        });
    }

    public static synchronized void clearPlayerPlaced(BlockLocation location) {
        withPlacedBlockTracker(tracker -> {
            tracker.clear(location);
            return false;
        });
    }

    public static synchronized boolean running() {
        return runtime != null;
    }

    public static synchronized void stop() {
        if (runtime == null && placedBlockTracker == null) {
            return;
        }
        FabricMmoServerRuntime activeRuntime = runtime;
        PlacedBlockTracker activeTracker = placedBlockTracker;
        runtime = null;
        miningXpTable = null;
        miningDropSettings = null;
        placedBlockTracker = null;

        IOException failure = null;
        if (activeTracker != null) {
            try {
                activeTracker.close();
            } catch (IOException exception) {
                failure = exception;
            }
        }
        if (activeRuntime != null) {
            try {
                activeRuntime.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw new UncheckedIOException("Unable to stop FabricMMO persistence", failure);
        }
        LOGGER.info("Stopped FabricMMO server runtime");
    }

    private static boolean withPlacedBlockTracker(PlacedBlockOperation operation) {
        if (placedBlockTracker == null) {
            throw new IllegalStateException("FabricMMO placed-block tracker is not active");
        }
        try {
            return operation.apply(placedBlockTracker);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to update FabricMMO placed-block data", exception);
        }
    }

    private static void closeAfterFailedStart(AutoCloseable closeable, Throwable failure) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception closeException) {
            failure.addSuppressed(closeException);
        }
    }

    @FunctionalInterface
    private interface PlacedBlockOperation {
        boolean apply(PlacedBlockTracker tracker) throws IOException;
    }
}

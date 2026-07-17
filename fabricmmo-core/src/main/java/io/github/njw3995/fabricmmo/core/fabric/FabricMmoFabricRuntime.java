package io.github.njw3995.fabricmmo.core.fabric;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.entrypoint.FabricMmoEntrypoint;
import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.block.NullPlacedBlockTracker;
import io.github.njw3995.fabricmmo.core.block.PlacedBlockSettings;
import io.github.njw3995.fabricmmo.core.block.PlacedBlockTracker;
import io.github.njw3995.fabricmmo.core.block.RegionPlacedBlockTracker;
import io.github.njw3995.fabricmmo.core.block.TrackedWorld;
import io.github.njw3995.fabricmmo.core.config.WorldBlacklist;
import io.github.njw3995.fabricmmo.core.progression.ProgressionSettings;
import io.github.njw3995.fabricmmo.core.runtime.FabricMmoServerRuntime;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningAbilityController;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningAbilityStateView;
import io.github.njw3995.fabricmmo.core.skill.mining.CoreMiningAbilities;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastRegistry;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningDropSettings;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningSettings;
import io.github.njw3995.fabricmmo.core.skill.mining.PropertiesMiningAbilityStore;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningXpTable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.dimension.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FabricMmoFabricRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO");
    private static FabricMmoServerRuntime runtime;
    private static MiningXpTable miningXpTable;
    private static MiningDropSettings miningDropSettings;
    private static PlacedBlockTracker placedBlockTracker;
    private static MiningSettings miningSettings;
    private static MiningAbilityController miningAbilityController;
    private static Path serverWorldRoot;
    private static WorldBlacklist worldBlacklist;
    private static ProgressionSettings progressionSettings;

    private FabricMmoFabricRuntime() {
    }

    public static synchronized void start(MinecraftServer server) {
        if (runtime != null) {
            throw new IllegalStateException("FabricMMO server runtime is already active");
        }

        Path worldRoot = server.getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
        Path playerDataDirectory = resolvePlayerDataDirectory(worldRoot);
        Path placedBlockDirectory = resolvePlacedBlockDirectory(worldRoot);
        Path miningAbilityDirectory = resolveMiningAbilityDirectory(worldRoot);
        Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve("fabricmmo");
        Path experienceFile = configDirectory.resolve("experience.yml");
        Path configFile = configDirectory.resolve("config.yml");
        Path advancedFile = configDirectory.resolve("advanced.yml");
        Path skillRanksFile = configDirectory.resolve("skillranks.yml");
        Path persistentDataFile = configDirectory.resolve("persistent_data.yml");
        Path worldBlacklistFile = configDirectory.resolve("world_blacklist.txt");

        FabricMmoServerRuntime newRuntime = null;
        PlacedBlockTracker newTracker = null;
        MiningAbilityController newAbilityController = null;
        try {
            ProgressionSettings progressionSettings = ProgressionSettings.load(
                    configFile, experienceFile);
            WorldBlacklist newWorldBlacklist = WorldBlacklist.load(worldBlacklistFile, worldRoot);
            MiningXpTable newXpTable = MiningXpTable.loadConfigured(experienceFile);
            MiningDropSettings newDropSettings = MiningDropSettings.load(
                    configFile, advancedFile, skillRanksFile);
            MiningSettings newMiningSettings = MiningSettings.load(
                    configFile, advancedFile, skillRanksFile, experienceFile);
            newAbilityController = new MiningAbilityController(
                    new PropertiesMiningAbilityStore(miningAbilityDirectory), Clock.systemUTC());
            PlacedBlockSettings placedBlockSettings = PlacedBlockSettings.load(persistentDataFile);
            newTracker = placedBlockSettings.regionSystemEnabled()
                    ? new RegionPlacedBlockTracker(placedBlockDirectory)
                    : new NullPlacedBlockTracker();
            for (ServerWorld world : server.getWorlds()) {
                newWorldBlacklist.register(world);
                newTracker.registerWorld(trackedWorld(world, worldRoot));
            }
            newRuntime = FabricMmoServerRuntime.start(
                    playerDataDirectory,
                    progressionSettings,
                    api -> FabricLoader.getInstance().invokeEntrypoints(
                            FabricMmoEntrypoint.KEY,
                            FabricMmoEntrypoint.class,
                            entrypoint -> entrypoint.register(api)));
            MiningAbilityStateView miningAbilityStates = new MiningAbilityStateView(
                    server, newAbilityController, newMiningSettings);
            newRuntime.api().abilityPipeline().registerStateView(
                    CoreMiningAbilities.SUPER_BREAKER, miningAbilityStates);
            newRuntime.api().abilityPipeline().registerStateView(
                    CoreMiningAbilities.BLAST_MINING, miningAbilityStates);
            runtime = newRuntime;
            miningXpTable = newXpTable;
            miningDropSettings = newDropSettings;
            placedBlockTracker = newTracker;
            miningSettings = newMiningSettings;
            miningAbilityController = newAbilityController;
            serverWorldRoot = worldRoot;
            worldBlacklist = newWorldBlacklist;
            FabricMmoFabricRuntime.progressionSettings = progressionSettings;
            LOGGER.info(
                    "Started with {} registered skills; player data directory: {}; placed-block directory: {}; Mining ability directory: {}",
                    runtime.api().skillRegistry().skills().size(),
                    playerDataDirectory,
                    placedBlockDirectory,
                    miningAbilityDirectory);
        } catch (IOException exception) {
            closeAfterFailedStart(newRuntime, exception);
            closeAfterFailedStart(newAbilityController, exception);
            closeAfterFailedStart(newTracker, exception);
            throw new UncheckedIOException("Unable to start FabricMMO persistence", exception);
        } catch (RuntimeException | Error exception) {
            closeAfterFailedStart(newRuntime, exception);
            closeAfterFailedStart(newAbilityController, exception);
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


    static Path resolveMiningAbilityDirectory(Path worldRoot) {
        Objects.requireNonNull(worldRoot, "worldRoot");
        return worldRoot.resolve("fabricmmo")
                .resolve("mining-abilities")
                .toAbsolutePath()
                .normalize();
    }

    public static synchronized FabricMmoApi requireApi() {
        if (runtime == null) {
            throw new IllegalStateException("FabricMMO server runtime is not active");
        }
        return runtime.api();
    }


    public static synchronized ProgressionSettings progressionSettings() {
        if (progressionSettings == null) {
            throw new IllegalStateException("FabricMMO progression settings are not active");
        }
        return progressionSettings;
    }

    public static synchronized MiningSettings miningSettings() {
        if (miningSettings == null) {
            throw new IllegalStateException("FabricMMO Mining settings are not active");
        }
        return miningSettings;
    }

    public static synchronized MiningAbilityController miningAbilities() {
        if (miningAbilityController == null) {
            throw new IllegalStateException("FabricMMO Mining ability runtime is not active");
        }
        return miningAbilityController;
    }

    public static synchronized boolean isSuperBreakerActive(UUID playerId) {
        try {
            return miningAbilities().isSuperBreakerActive(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Mining ability state", exception);
        }
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

    public static synchronized boolean isWorldBlacklisted(ServerWorld world) {
        return worldBlacklist != null && worldBlacklist.isBlacklisted(world);
    }

    public static synchronized boolean isWorldBlacklisted(String worldId) {
        return worldBlacklist != null && worldBlacklist.isBlacklisted(worldId);
    }

    public static synchronized boolean isPlayerPlaced(BlockLocation location) {
        return requirePlacedBlockTracker().isIneligible(location);
    }

    public static synchronized void markPlayerPlaced(BlockLocation location) {
        requirePlacedBlockTracker().setIneligible(location);
    }

    public static synchronized void clearPlayerPlaced(BlockLocation location) {
        requirePlacedBlockTracker().setEligible(location);
    }

    public static synchronized void worldLoaded(ServerWorld world) {
        if (placedBlockTracker == null || serverWorldRoot == null) {
            return;
        }
        try {
            if (worldBlacklist != null) {
                worldBlacklist.register(world);
            }
            placedBlockTracker.registerWorld(trackedWorld(world, serverWorldRoot));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to register FabricMMO world storage", exception);
        }
    }

    public static synchronized void worldUnloaded(ServerWorld world) {
        if (placedBlockTracker == null) {
            return;
        }
        placedBlockTracker.unloadWorld(worldId(world));
        if (worldBlacklist != null) {
            worldBlacklist.unregister(world);
        }
    }

    public static synchronized void chunkUnloaded(ServerWorld world, int chunkX, int chunkZ) {
        if (placedBlockTracker == null) {
            return;
        }
        placedBlockTracker.chunkUnloaded(worldId(world), chunkX, chunkZ);
    }

    public static synchronized boolean running() {
        return runtime != null;
    }

    public static synchronized void stop() {
        if (runtime == null && placedBlockTracker == null && miningAbilityController == null) {
            return;
        }
        FabricMmoServerRuntime activeRuntime = runtime;
        PlacedBlockTracker activeTracker = placedBlockTracker;
        MiningAbilityController activeAbilityController = miningAbilityController;
        MiningBlastRegistry.clear();
        MiningAbilityHandler.reset();
        runtime = null;
        miningXpTable = null;
        miningDropSettings = null;
        placedBlockTracker = null;
        miningSettings = null;
        miningAbilityController = null;
        serverWorldRoot = null;
        worldBlacklist = null;
        progressionSettings = null;

        IOException failure = null;
        if (activeAbilityController != null) {
            try {
                activeAbilityController.close();
            } catch (IOException exception) {
                failure = exception;
            }
        }
        if (activeTracker != null) {
            activeTracker.close();
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

    private static PlacedBlockTracker requirePlacedBlockTracker() {
        if (placedBlockTracker == null) {
            throw new IllegalStateException("FabricMMO placed-block tracker is not active");
        }
        return placedBlockTracker;
    }

    private static TrackedWorld trackedWorld(ServerWorld world, Path worldRoot) {
        String worldId = worldId(world);
        Path dimensionDirectory = DimensionType.getSaveDirectory(world.getRegistryKey(), worldRoot);
        return new TrackedWorld(
                worldId,
                dimensionDirectory.resolve("fabricmmo_regions"),
                world.getBottomY(),
                world.getTopY());
    }

    private static String worldId(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
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
}

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
import io.github.njw3995.fabricmmo.core.persistence.MySqlSettings;
import io.github.njw3995.fabricmmo.core.player.PlayerSessionSettingsService;
import io.github.njw3995.fabricmmo.core.runtime.FabricMmoServerRuntime;
import io.github.njw3995.fabricmmo.core.skill.acrobatics.AcrobaticsRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.acrobatics.AcrobaticsSettings;
import io.github.njw3995.fabricmmo.core.skill.alchemy.AlchemyPotionConfig;
import io.github.njw3995.fabricmmo.core.skill.alchemy.AlchemyRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.alchemy.AlchemySettings;
import io.github.njw3995.fabricmmo.core.skill.excavation.CoreExcavationAbilities;
import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationAbilityController;
import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationAbilityStateView;
import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationSettings;
import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationTreasureTable;
import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationXpDefaults;
import io.github.njw3995.fabricmmo.core.skill.excavation.PropertiesExcavationAbilityStore;
import io.github.njw3995.fabricmmo.core.skill.herbalism.CoreHerbalismAbilities;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismAbilityController;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismAbilityStateView;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismBlockBreakHandler;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismDropSettings;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismFoodHandler;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismHylianTreasureTable;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismInteractionHandler;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismSettings;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismXpDefaults;
import io.github.njw3995.fabricmmo.core.skill.herbalism.PropertiesHerbalismAbilityStore;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningAbilityController;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningAbilityStateView;
import io.github.njw3995.fabricmmo.core.skill.mining.CoreMiningAbilities;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastRegistry;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningDropSettings;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningSettings;
import io.github.njw3995.fabricmmo.core.skill.mining.PropertiesMiningAbilityStore;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningXpTable;
import io.github.njw3995.fabricmmo.core.skill.gathering.ConfiguredBlockXpTable;
import io.github.njw3995.fabricmmo.core.skill.fishing.FishingAntiExploitTracker;
import io.github.njw3995.fabricmmo.core.skill.fishing.FishingFoodHandler;
import io.github.njw3995.fabricmmo.core.skill.fishing.FishingRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.fishing.FishingSettings;
import io.github.njw3995.fabricmmo.core.skill.fishing.FishingTreasureTable;
import io.github.njw3995.fabricmmo.core.skill.fishing.FishingXpTable;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.CoreWoodcuttingAbilities;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.PropertiesWoodcuttingAbilityStore;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingAbilityController;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingAbilityStateView;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingDropSettings;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingSettings;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingXpDefaults;
import io.github.njw3995.fabricmmo.core.skill.combat.CombatXpSettings;
import io.github.njw3995.fabricmmo.core.skill.swords.CoreSwordsAbilities;
import io.github.njw3995.fabricmmo.core.skill.swords.PropertiesSwordsAbilityStore;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsAbilityController;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsAbilityStateView;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsSettings;
import io.github.njw3995.fabricmmo.core.skill.taming.TamingSettings;
import io.github.njw3995.fabricmmo.core.skill.taming.TamingXpTable;
import io.github.njw3995.fabricmmo.core.skill.taming.TamingRuntimeHandler;
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
    private static AcrobaticsSettings acrobaticsSettings;
    private static MiningXpTable miningXpTable;
    private static MiningDropSettings miningDropSettings;
    private static PlacedBlockTracker placedBlockTracker;
    private static MiningSettings miningSettings;
    private static MiningAbilityController miningAbilityController;
    private static ConfiguredBlockXpTable woodcuttingXpTable;
    private static WoodcuttingDropSettings woodcuttingDropSettings;
    private static WoodcuttingSettings woodcuttingSettings;
    private static WoodcuttingAbilityController woodcuttingAbilityController;
    private static ConfiguredBlockXpTable excavationXpTable;
    private static ExcavationSettings excavationSettings;
    private static ExcavationTreasureTable excavationTreasures;
    private static ExcavationAbilityController excavationAbilityController;
    private static ConfiguredBlockXpTable herbalismXpTable;
    private static HerbalismDropSettings herbalismDropSettings;
    private static HerbalismSettings herbalismSettings;
    private static HerbalismHylianTreasureTable herbalismTreasures;
    private static HerbalismAbilityController herbalismAbilityController;
    private static FishingXpTable fishingXpTable;
    private static FishingSettings fishingSettings;
    private static FishingTreasureTable fishingTreasures;
    private static FishingAntiExploitTracker fishingAntiExploit;
    private static CombatXpSettings combatXpSettings;
    private static SwordsSettings swordsSettings;
    private static SwordsAbilityController swordsAbilityController;
    private static TamingSettings tamingSettings;
    private static TamingXpTable tamingXpTable;
    private static AlchemySettings alchemySettings;
    private static AlchemyPotionConfig alchemyPotionConfig;
    private static Path serverWorldRoot;
    private static WorldBlacklist worldBlacklist;
    private static ProgressionSettings progressionSettings;
    private static PlayerSessionSettingsService playerSessionSettings;

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
        Path woodcuttingAbilityDirectory = resolveWoodcuttingAbilityDirectory(worldRoot);
        Path excavationAbilityDirectory = resolveExcavationAbilityDirectory(worldRoot);
        Path herbalismAbilityDirectory = resolveHerbalismAbilityDirectory(worldRoot);
        Path swordsAbilityDirectory = resolveSwordsAbilityDirectory(worldRoot);
        Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve("fabricmmo");
        Path experienceFile = configDirectory.resolve("experience.yml");
        Path configFile = configDirectory.resolve("config.yml");
        Path advancedFile = configDirectory.resolve("advanced.yml");
        Path skillRanksFile = configDirectory.resolve("skillranks.yml");
        Path treasuresFile = configDirectory.resolve("treasures.yml");
        Path fishingTreasuresFile = configDirectory.resolve("fishing_treasures.yml");
        Path potionsFile = configDirectory.resolve("potions.yml");
        Path soundsFile = configDirectory.resolve("sounds.yml");
        Path persistentDataFile = configDirectory.resolve("persistent_data.yml");
        Path worldBlacklistFile = configDirectory.resolve("world_blacklist.txt");

        FabricMmoServerRuntime newRuntime = null;
        PlacedBlockTracker newTracker = null;
        MiningAbilityController newAbilityController = null;
        WoodcuttingAbilityController newWoodcuttingAbilityController = null;
        ExcavationAbilityController newExcavationAbilityController = null;
        HerbalismAbilityController newHerbalismAbilityController = null;
        SwordsAbilityController newSwordsAbilityController = null;
        try {
            ProgressionSettings progressionSettings = ProgressionSettings.load(
                    configFile, experienceFile);
            AcrobaticsSettings newAcrobaticsSettings = AcrobaticsSettings.load(
                    configFile, advancedFile, skillRanksFile, experienceFile, soundsFile);
            WorldBlacklist newWorldBlacklist = WorldBlacklist.load(worldBlacklistFile, worldRoot);
            MiningXpTable newXpTable = MiningXpTable.loadConfigured(experienceFile);
            MiningDropSettings newDropSettings = MiningDropSettings.load(
                    configFile, advancedFile, skillRanksFile);
            MiningSettings newMiningSettings = MiningSettings.load(
                    configFile, advancedFile, skillRanksFile, experienceFile);
            ConfiguredBlockXpTable newWoodcuttingXpTable =
                    ConfiguredBlockXpTable.load(
                            experienceFile, "Woodcutting", WoodcuttingXpDefaults.values());
            WoodcuttingDropSettings newWoodcuttingDropSettings = WoodcuttingDropSettings.load(
                    configFile, advancedFile, skillRanksFile);
            WoodcuttingSettings newWoodcuttingSettings = WoodcuttingSettings.load(
                    configFile, advancedFile, skillRanksFile, experienceFile);
            ConfiguredBlockXpTable newExcavationXpTable = ConfiguredBlockXpTable.load(
                    experienceFile, "Excavation", ExcavationXpDefaults.values());
            ExcavationSettings newExcavationSettings = ExcavationSettings.load(
                    configFile, advancedFile, skillRanksFile);
            ExcavationTreasureTable newExcavationTreasures = ExcavationTreasureTable.load(
                    treasuresFile);
            ConfiguredBlockXpTable newHerbalismXpTable = ConfiguredBlockXpTable.load(
                    experienceFile, "Herbalism", HerbalismXpDefaults.values());
            HerbalismDropSettings newHerbalismDropSettings = HerbalismDropSettings.load(configFile);
            HerbalismSettings newHerbalismSettings = HerbalismSettings.load(
                    configFile, advancedFile, skillRanksFile, experienceFile);
            HerbalismHylianTreasureTable newHerbalismTreasures =
                    HerbalismHylianTreasureTable.load(treasuresFile);
            FishingXpTable newFishingXpTable = FishingXpTable.load(experienceFile);
            FishingSettings newFishingSettings = FishingSettings.load(
                    configFile, advancedFile, skillRanksFile, experienceFile);
            FishingTreasureTable newFishingTreasures = FishingTreasureTable.load(
                    fishingTreasuresFile);
            FishingAntiExploitTracker newFishingAntiExploit = new FishingAntiExploitTracker(
                    newFishingSettings.exploitMoveRange(), newFishingSettings.overFishLimit());
            CombatXpSettings newCombatXpSettings = CombatXpSettings.load(experienceFile);
            SwordsSettings newSwordsSettings = SwordsSettings.load(
                    configFile, advancedFile, skillRanksFile, soundsFile);
            TamingSettings newTamingSettings = TamingSettings.load(
                    configFile, advancedFile, skillRanksFile, experienceFile,
                    progressionSettings.mode());
            TamingXpTable newTamingXpTable = TamingXpTable.load(experienceFile);
            AlchemySettings newAlchemySettings = AlchemySettings.load(
                    configFile, advancedFile, skillRanksFile, experienceFile, progressionSettings.mode());
            AlchemyPotionConfig newAlchemyPotionConfig = AlchemyPotionConfig.load(potionsFile);
            newAbilityController = new MiningAbilityController(
                    new PropertiesMiningAbilityStore(miningAbilityDirectory), Clock.systemUTC());
            newWoodcuttingAbilityController = new WoodcuttingAbilityController(
                    new PropertiesWoodcuttingAbilityStore(woodcuttingAbilityDirectory),
                    Clock.systemUTC());
            newExcavationAbilityController = new ExcavationAbilityController(
                    new PropertiesExcavationAbilityStore(excavationAbilityDirectory),
                    Clock.systemUTC());
            newHerbalismAbilityController = new HerbalismAbilityController(
                    new PropertiesHerbalismAbilityStore(herbalismAbilityDirectory),
                    Clock.systemUTC());
            newSwordsAbilityController = new SwordsAbilityController(
                    new PropertiesSwordsAbilityStore(swordsAbilityDirectory), Clock.systemUTC());
            PlacedBlockSettings placedBlockSettings = PlacedBlockSettings.load(persistentDataFile);
            newTracker = placedBlockSettings.regionSystemEnabled()
                    ? new RegionPlacedBlockTracker(placedBlockDirectory)
                    : new NullPlacedBlockTracker();
            for (ServerWorld world : server.getWorlds()) {
                newWorldBlacklist.register(world);
                newTracker.registerWorld(trackedWorld(world, worldRoot));
            }
            MySqlSettings mySqlSettings = MySqlSettings.load(configFile);
            newRuntime = FabricMmoServerRuntime.start(
                    playerDataDirectory,
                    progressionSettings,
                    mySqlSettings,
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
            WoodcuttingAbilityStateView woodcuttingAbilityStates = new WoodcuttingAbilityStateView(
                    server, newWoodcuttingAbilityController, newWoodcuttingSettings);
            newRuntime.api().abilityPipeline().registerStateView(
                    CoreWoodcuttingAbilities.TREE_FELLER, woodcuttingAbilityStates);
            ExcavationAbilityStateView excavationAbilityStates = new ExcavationAbilityStateView(
                    server, newExcavationAbilityController, newExcavationSettings);
            newRuntime.api().abilityPipeline().registerStateView(
                    CoreExcavationAbilities.GIGA_DRILL_BREAKER, excavationAbilityStates);
            HerbalismAbilityStateView herbalismAbilityStates = new HerbalismAbilityStateView(
                    server, newHerbalismAbilityController, newHerbalismSettings);
            newRuntime.api().abilityPipeline().registerStateView(
                    CoreHerbalismAbilities.GREEN_TERRA, herbalismAbilityStates);
            SwordsAbilityStateView swordsAbilityStates = new SwordsAbilityStateView(
                    server, newSwordsAbilityController, newSwordsSettings);
            newRuntime.api().abilityPipeline().registerStateView(
                    CoreSwordsAbilities.SERRATED_STRIKES, swordsAbilityStates);
            SharedServerSystems.start(
                    server,
                    worldRoot,
                    playerDataDirectory,
                    configDirectory,
                    newRuntime.api(),
                    newRuntime.store(),
                    mySqlSettings,
                    progressionSettings,
                    newAcrobaticsSettings,
                    newAbilityController,
                    newMiningSettings,
                    newDropSettings,
                    newWoodcuttingAbilityController,
                    newWoodcuttingSettings,
                    newWoodcuttingDropSettings,
                    newExcavationAbilityController,
                    newExcavationSettings,
                    newHerbalismAbilityController,
                    newHerbalismSettings,
                    newHerbalismDropSettings,
                    newFishingSettings,
                    newFishingTreasures,
                    newSwordsAbilityController,
                    newSwordsSettings,
                    newTamingSettings,
                    newAlchemySettings);
            runtime = newRuntime;
            acrobaticsSettings = newAcrobaticsSettings;
            miningXpTable = newXpTable;
            miningDropSettings = newDropSettings;
            placedBlockTracker = newTracker;
            miningSettings = newMiningSettings;
            miningAbilityController = newAbilityController;
            woodcuttingXpTable = newWoodcuttingXpTable;
            woodcuttingDropSettings = newWoodcuttingDropSettings;
            woodcuttingSettings = newWoodcuttingSettings;
            woodcuttingAbilityController = newWoodcuttingAbilityController;
            excavationXpTable = newExcavationXpTable;
            excavationSettings = newExcavationSettings;
            excavationTreasures = newExcavationTreasures;
            excavationAbilityController = newExcavationAbilityController;
            herbalismXpTable = newHerbalismXpTable;
            herbalismDropSettings = newHerbalismDropSettings;
            herbalismSettings = newHerbalismSettings;
            herbalismTreasures = newHerbalismTreasures;
            herbalismAbilityController = newHerbalismAbilityController;
            fishingXpTable = newFishingXpTable;
            fishingSettings = newFishingSettings;
            fishingTreasures = newFishingTreasures;
            fishingAntiExploit = newFishingAntiExploit;
            combatXpSettings = newCombatXpSettings;
            swordsSettings = newSwordsSettings;
            swordsAbilityController = newSwordsAbilityController;
            tamingSettings = newTamingSettings;
            tamingXpTable = newTamingXpTable;
            alchemySettings = newAlchemySettings;
            alchemyPotionConfig = newAlchemyPotionConfig;
            serverWorldRoot = worldRoot;
            worldBlacklist = newWorldBlacklist;
            FabricMmoFabricRuntime.progressionSettings = progressionSettings;
            playerSessionSettings = new PlayerSessionSettingsService();
            LOGGER.info(
                    "Started with {} registered skills; player data directory: {}; placed-block directory: {}; Mining ability directory: {}; Woodcutting ability directory: {}; Excavation ability directory: {}; Herbalism ability directory: {}; Swords ability directory: {}",
                    runtime.api().skillRegistry().skills().size(),
                    playerDataDirectory,
                    placedBlockDirectory,
                    miningAbilityDirectory,
                    woodcuttingAbilityDirectory,
                    excavationAbilityDirectory,
                    herbalismAbilityDirectory,
                    swordsAbilityDirectory);
        } catch (IOException exception) {
            closeAfterFailedStart(newRuntime, exception);
            closeAfterFailedStart(newAbilityController, exception);
            closeAfterFailedStart(newWoodcuttingAbilityController, exception);
            closeAfterFailedStart(newExcavationAbilityController, exception);
            closeAfterFailedStart(newHerbalismAbilityController, exception);
            closeAfterFailedStart(newSwordsAbilityController, exception);
            closeAfterFailedStart(newTracker, exception);
            throw new UncheckedIOException("Unable to start FabricMMO persistence", exception);
        } catch (RuntimeException | Error exception) {
            closeAfterFailedStart(newRuntime, exception);
            closeAfterFailedStart(newAbilityController, exception);
            closeAfterFailedStart(newWoodcuttingAbilityController, exception);
            closeAfterFailedStart(newExcavationAbilityController, exception);
            closeAfterFailedStart(newHerbalismAbilityController, exception);
            closeAfterFailedStart(newSwordsAbilityController, exception);
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

    static Path resolveWoodcuttingAbilityDirectory(Path worldRoot) {
        Objects.requireNonNull(worldRoot, "worldRoot");
        return worldRoot.resolve("fabricmmo")
                .resolve("woodcutting-abilities")
                .toAbsolutePath()
                .normalize();
    }

    static Path resolveExcavationAbilityDirectory(Path worldRoot) {
        Objects.requireNonNull(worldRoot, "worldRoot");
        return worldRoot.resolve("fabricmmo")
                .resolve("excavation-abilities")
                .toAbsolutePath()
                .normalize();
    }

    static Path resolveHerbalismAbilityDirectory(Path worldRoot) {
        Objects.requireNonNull(worldRoot, "worldRoot");
        return worldRoot.resolve("fabricmmo")
                .resolve("herbalism-abilities")
                .toAbsolutePath()
                .normalize();
    }

    static Path resolveSwordsAbilityDirectory(Path worldRoot) {
        Objects.requireNonNull(worldRoot, "worldRoot");
        return worldRoot.resolve("fabricmmo")
                .resolve("swords-abilities")
                .toAbsolutePath()
                .normalize();
    }

    public static synchronized FabricMmoApi requireApi() {
        if (runtime == null) {
            throw new IllegalStateException("FabricMMO server runtime is not active");
        }
        return runtime.api();
    }


    public static synchronized AcrobaticsSettings acrobaticsSettings() {
        if (acrobaticsSettings == null) {
            throw new IllegalStateException("FabricMMO Acrobatics settings are not active");
        }
        return acrobaticsSettings;
    }

    public static synchronized PlayerSessionSettingsService playerSessionSettings() {
        if (playerSessionSettings == null) {
            throw new IllegalStateException("FabricMMO player session settings are not active");
        }
        return playerSessionSettings;
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

    public static synchronized ConfiguredBlockXpTable woodcuttingXpTable() {
        if (woodcuttingXpTable == null) {
            throw new IllegalStateException("FabricMMO Woodcutting XP configuration is not active");
        }
        return woodcuttingXpTable;
    }

    public static synchronized int woodcuttingXpFor(NamespacedId blockId) {
        return woodcuttingXpTable().xpFor(blockId);
    }

    public static synchronized WoodcuttingDropSettings woodcuttingDropSettings() {
        if (woodcuttingDropSettings == null) {
            throw new IllegalStateException("FabricMMO Woodcutting drop configuration is not active");
        }
        return woodcuttingDropSettings;
    }

    public static synchronized WoodcuttingSettings woodcuttingSettings() {
        if (woodcuttingSettings == null) {
            throw new IllegalStateException("FabricMMO Woodcutting settings are not active");
        }
        return woodcuttingSettings;
    }

    public static synchronized WoodcuttingAbilityController woodcuttingAbilities() {
        if (woodcuttingAbilityController == null) {
            throw new IllegalStateException("FabricMMO Woodcutting ability runtime is not active");
        }
        return woodcuttingAbilityController;
    }

    public static synchronized boolean isTreeFellerActive(UUID playerId) {
        try {
            return woodcuttingAbilities().isActive(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Woodcutting ability state", exception);
        }
    }

    public static synchronized ConfiguredBlockXpTable excavationXpTable() {
        if (excavationXpTable == null) {
            throw new IllegalStateException("FabricMMO Excavation XP configuration is not active");
        }
        return excavationXpTable;
    }

    public static synchronized int excavationXpFor(NamespacedId blockId) {
        return excavationXpTable().xpFor(blockId);
    }

    public static synchronized ExcavationSettings excavationSettings() {
        if (excavationSettings == null) {
            throw new IllegalStateException("FabricMMO Excavation settings are not active");
        }
        return excavationSettings;
    }

    public static synchronized ExcavationTreasureTable excavationTreasures() {
        if (excavationTreasures == null) {
            throw new IllegalStateException("FabricMMO Excavation treasures are not active");
        }
        return excavationTreasures;
    }

    public static synchronized ExcavationAbilityController excavationAbilities() {
        if (excavationAbilityController == null) {
            throw new IllegalStateException("FabricMMO Excavation ability runtime is not active");
        }
        return excavationAbilityController;
    }

    public static synchronized boolean isGigaDrillBreakerActive(UUID playerId) {
        try {
            return excavationAbilities().isActive(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Excavation ability state", exception);
        }
    }


    public static synchronized ConfiguredBlockXpTable herbalismXpTable() {
        if (herbalismXpTable == null) {
            throw new IllegalStateException("FabricMMO Herbalism XP configuration is not active");
        }
        return herbalismXpTable;
    }

    public static synchronized int herbalismXpFor(NamespacedId blockId) {
        return herbalismXpTable().xpFor(blockId);
    }

    public static synchronized HerbalismDropSettings herbalismDropSettings() {
        if (herbalismDropSettings == null) {
            throw new IllegalStateException("FabricMMO Herbalism drop configuration is not active");
        }
        return herbalismDropSettings;
    }

    public static synchronized HerbalismSettings herbalismSettings() {
        if (herbalismSettings == null) {
            throw new IllegalStateException("FabricMMO Herbalism settings are not active");
        }
        return herbalismSettings;
    }

    public static synchronized HerbalismHylianTreasureTable herbalismTreasures() {
        if (herbalismTreasures == null) {
            throw new IllegalStateException("FabricMMO Herbalism treasures are not active");
        }
        return herbalismTreasures;
    }

    public static synchronized HerbalismAbilityController herbalismAbilities() {
        if (herbalismAbilityController == null) {
            throw new IllegalStateException("FabricMMO Herbalism ability runtime is not active");
        }
        return herbalismAbilityController;
    }

    public static synchronized boolean isGreenTerraActive(UUID playerId) {
        try {
            return herbalismAbilities().isActive(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Herbalism ability state", exception);
        }
    }

    public static synchronized FishingXpTable fishingXpTable() {
        if (fishingXpTable == null) {
            throw new IllegalStateException("FabricMMO Fishing XP configuration is not active");
        }
        return fishingXpTable;
    }

    public static synchronized FishingSettings fishingSettings() {
        if (fishingSettings == null) {
            throw new IllegalStateException("FabricMMO Fishing settings are not active");
        }
        return fishingSettings;
    }

    public static synchronized FishingTreasureTable fishingTreasures() {
        if (fishingTreasures == null) {
            throw new IllegalStateException("FabricMMO Fishing treasures are not active");
        }
        return fishingTreasures;
    }

    public static synchronized FishingAntiExploitTracker fishingAntiExploit() {
        if (fishingAntiExploit == null) {
            throw new IllegalStateException("FabricMMO Fishing anti-exploit state is not active");
        }
        return fishingAntiExploit;
    }

    public static synchronized CombatXpSettings combatXpSettings() {
        if (combatXpSettings == null) {
            throw new IllegalStateException("FabricMMO combat XP settings are not active");
        }
        return combatXpSettings;
    }

    public static synchronized SwordsSettings swordsSettings() {
        if (swordsSettings == null) {
            throw new IllegalStateException("FabricMMO Swords settings are not active");
        }
        return swordsSettings;
    }

    public static synchronized SwordsAbilityController swordsAbilities() {
        if (swordsAbilityController == null) {
            throw new IllegalStateException("FabricMMO Swords ability runtime is not active");
        }
        return swordsAbilityController;
    }


    public static synchronized TamingSettings tamingSettings() {
        if (tamingSettings == null) {
            throw new IllegalStateException("FabricMMO Taming settings are not active");
        }
        return tamingSettings;
    }

    public static synchronized TamingXpTable tamingXpTable() {
        if (tamingXpTable == null) {
            throw new IllegalStateException("FabricMMO Taming XP table is not active");
        }
        return tamingXpTable;
    }


    public static synchronized AlchemySettings alchemySettings() {
        if (alchemySettings == null) {
            throw new IllegalStateException("FabricMMO Alchemy settings are not active");
        }
        return alchemySettings;
    }

    public static synchronized AlchemyPotionConfig alchemyPotionConfig() {
        if (alchemyPotionConfig == null) {
            throw new IllegalStateException("FabricMMO Alchemy potion configuration is not active");
        }
        return alchemyPotionConfig;
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
        if (runtime == null
                && placedBlockTracker == null
                && miningAbilityController == null
                && woodcuttingAbilityController == null
                && excavationAbilityController == null
                && herbalismAbilityController == null
                && swordsAbilityController == null) {
            return;
        }
        MinecraftServer activeServer = SharedServerSystems.running()
                ? SharedServerSystems.require().server() : null;
        if (activeServer != null) {
            AlchemyRuntimeHandler.finishAll(activeServer);
            TamingRuntimeHandler.reset(activeServer);
        }
        SharedServerSystems.stop();
        FabricMmoServerRuntime activeRuntime = runtime;
        PlacedBlockTracker activeTracker = placedBlockTracker;
        MiningAbilityController activeAbilityController = miningAbilityController;
        WoodcuttingAbilityController activeWoodcuttingAbilityController =
                woodcuttingAbilityController;
        ExcavationAbilityController activeExcavationAbilityController =
                excavationAbilityController;
        HerbalismAbilityController activeHerbalismAbilityController = herbalismAbilityController;
        SwordsAbilityController activeSwordsAbilityController = swordsAbilityController;
        MiningBlastRegistry.clear();
        MiningAbilityHandler.reset();
        WoodcuttingAbilityHandler.reset();
        ExcavationAbilityHandler.reset();
        HerbalismAbilityHandler.reset();
        HerbalismBlockBreakHandler.reset();
        HerbalismInteractionHandler.reset();
        HerbalismFoodHandler.reset();
        FishingFoodHandler.reset();
        FishingRuntimeHandler.reset();
        AcrobaticsRuntimeHandler.clear();
        SwordsAbilityHandler.reset();
        SwordsRuntimeHandler.reset();
        AlchemyRuntimeHandler.reset();
        runtime = null;
        acrobaticsSettings = null;
        miningXpTable = null;
        miningDropSettings = null;
        placedBlockTracker = null;
        miningSettings = null;
        miningAbilityController = null;
        woodcuttingXpTable = null;
        woodcuttingDropSettings = null;
        woodcuttingSettings = null;
        woodcuttingAbilityController = null;
        excavationXpTable = null;
        excavationSettings = null;
        excavationTreasures = null;
        excavationAbilityController = null;
        herbalismXpTable = null;
        herbalismDropSettings = null;
        herbalismSettings = null;
        herbalismTreasures = null;
        herbalismAbilityController = null;
        fishingXpTable = null;
        fishingSettings = null;
        fishingTreasures = null;
        fishingAntiExploit = null;
        combatXpSettings = null;
        swordsSettings = null;
        swordsAbilityController = null;
        tamingSettings = null;
        tamingXpTable = null;
        alchemySettings = null;
        alchemyPotionConfig = null;
        serverWorldRoot = null;
        worldBlacklist = null;
        progressionSettings = null;
        if (playerSessionSettings != null) {
            playerSessionSettings.clear();
        }
        playerSessionSettings = null;

        IOException failure = null;
        if (activeAbilityController != null) {
            try {
                activeAbilityController.close();
            } catch (IOException exception) {
                failure = exception;
            }
        }
        if (activeWoodcuttingAbilityController != null) {
            try {
                activeWoodcuttingAbilityController.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (activeExcavationAbilityController != null) {
            try {
                activeExcavationAbilityController.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (activeHerbalismAbilityController != null) {
            try {
                activeHerbalismAbilityController.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (activeSwordsAbilityController != null) {
            try {
                activeSwordsAbilityController.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
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

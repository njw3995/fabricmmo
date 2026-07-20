package io.github.njw3995.fabricmmo.core.fabric;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.event.AbilityStateEvent;
import io.github.njw3995.fabricmmo.api.event.EventSubscription;
import io.github.njw3995.fabricmmo.api.event.LevelChangedEvent;
import io.github.njw3995.fabricmmo.api.event.XpAwardedEvent;
import io.github.njw3995.fabricmmo.api.event.XpPreAwardEvent;
import io.github.njw3995.fabricmmo.core.ability.AbilityCooldownService;
import io.github.njw3995.fabricmmo.core.administration.ExperienceConversionService;
import io.github.njw3995.fabricmmo.core.administration.LegacyFlatFileImporter;
import io.github.njw3995.fabricmmo.core.administration.ProgressionAdminService;
import io.github.njw3995.fabricmmo.core.administration.ProgressionMaintenanceService;
import io.github.njw3995.fabricmmo.core.administration.ScheduledMaintenanceService;
import io.github.njw3995.fabricmmo.core.chat.ChatSettings;
import io.github.njw3995.fabricmmo.core.chat.ChatStateService;
import io.github.njw3995.fabricmmo.core.command.StatsTextFormatter;
import io.github.njw3995.fabricmmo.core.diagnostic.DebugDiagnosticsService;
import io.github.njw3995.fabricmmo.core.diagnostic.UiTraceLogger;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import io.github.njw3995.fabricmmo.core.info.SkillGuideCatalog;
import io.github.njw3995.fabricmmo.core.info.SkillPanelCooldownCatalog;
import io.github.njw3995.fabricmmo.core.info.SkillPanelMechanicsCatalog;
import io.github.njw3995.fabricmmo.core.info.SkillPanelService;
import io.github.njw3995.fabricmmo.core.info.SubSkillRankCatalog;
import io.github.njw3995.fabricmmo.core.leaderboard.LeaderboardService;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import io.github.njw3995.fabricmmo.core.party.ItemWeightSettings;
import io.github.njw3995.fabricmmo.core.party.PartyGameplayService;
import io.github.njw3995.fabricmmo.core.party.PartyService;
import io.github.njw3995.fabricmmo.core.party.PartySettings;
import io.github.njw3995.fabricmmo.core.party.PartyXpRuntime;
import io.github.njw3995.fabricmmo.core.party.PropertiesPartyStore;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.persistence.ManagedProgressionStore;
import io.github.njw3995.fabricmmo.core.persistence.MySqlSettings;
import io.github.njw3995.fabricmmo.core.player.PlayerIdentityStore;
import io.github.njw3995.fabricmmo.core.player.PlayerVisibilityService;
import io.github.njw3995.fabricmmo.core.progression.ProgressionFormula;
import io.github.njw3995.fabricmmo.core.progression.ProgressionSettings;
import io.github.njw3995.fabricmmo.core.session.PlayerSessionStateService;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import io.github.njw3995.fabricmmo.core.skill.acrobatics.AcrobaticsPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.skill.acrobatics.AcrobaticsSettings;
import io.github.njw3995.fabricmmo.core.skill.alchemy.AlchemyPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.skill.alchemy.AlchemySettings;
import io.github.njw3995.fabricmmo.core.skill.excavation.CoreExcavationAbilities;
import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationAbilityController;
import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationSettings;
import io.github.njw3995.fabricmmo.core.skill.herbalism.CoreHerbalismAbilities;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismAbilityController;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismDropSettings;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.skill.fishing.FishingPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.skill.fishing.FishingSettings;
import io.github.njw3995.fabricmmo.core.skill.fishing.FishingTreasureTable;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismSettings;
import io.github.njw3995.fabricmmo.core.skill.mining.CoreMiningAbilities;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningAbilityController;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningDropSettings;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningSettings;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.CoreWoodcuttingAbilities;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingAbilityController;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingDropSettings;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingSettings;
import io.github.njw3995.fabricmmo.core.skill.swords.CoreSwordsAbilities;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsAbilityController;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsSettings;
import io.github.njw3995.fabricmmo.core.skill.taming.TamingPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.skill.taming.TamingSettings;
import io.github.njw3995.fabricmmo.core.teleport.PartyTeleportService;
import io.github.njw3995.fabricmmo.core.ui.InMemoryPlayerUiSettingsStore;
import io.github.njw3995.fabricmmo.core.ui.PlayerScoreboardService;
import io.github.njw3995.fabricmmo.core.ui.ScoreboardTipService;
import io.github.njw3995.fabricmmo.core.ui.PlayerUiSettingsService;
import io.github.njw3995.fabricmmo.core.ui.XpBarMode;
import io.github.njw3995.fabricmmo.core.ui.XpBossBarService;
import io.github.njw3995.fabricmmo.core.ui.UiSettings;
import io.github.njw3995.fabricmmo.core.xprate.XpRateRuntime;
import io.github.njw3995.fabricmmo.core.xprate.XpRateService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-scoped shared command, UI, party, diagnostics, and maintenance services. */
public final class SharedServerSystems {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO");
    private static State state;

    private SharedServerSystems() {
    }

    public static synchronized void start(
            MinecraftServer server,
            Path worldRoot,
            Path playerDataDirectory,
            Path configDirectory,
            FabricMmoApi api,
            ManagedProgressionStore progressionStore,
            MySqlSettings mySqlSettings,
            ProgressionSettings progressionSettings,
            AcrobaticsSettings acrobaticsSettings,
            MiningAbilityController miningAbilities,
            MiningSettings miningSettings,
            MiningDropSettings miningDropSettings,
            WoodcuttingAbilityController woodcuttingAbilities,
            WoodcuttingSettings woodcuttingSettings,
            WoodcuttingDropSettings woodcuttingDropSettings,
            ExcavationAbilityController excavationAbilities,
            ExcavationSettings excavationSettings,
            HerbalismAbilityController herbalismAbilities,
            HerbalismSettings herbalismSettings,
            HerbalismDropSettings herbalismDropSettings,
            FishingSettings fishingSettings,
            FishingTreasureTable fishingTreasures,
            SwordsAbilityController swordsAbilities,
            SwordsSettings swordsSettings,
            TamingSettings tamingSettings,
            AlchemySettings alchemySettings) throws IOException {
        if (state != null) {
            throw new IllegalStateException("Shared FabricMMO systems already active");
        }
        Path dataRoot = worldRoot.resolve("fabricmmo");
        PlayerIdentityStore identities = new PlayerIdentityStore(dataRoot.resolve("player-index.properties"));
        backfillIdentities(server, progressionStore, identities);
        PartySettings partySettings = PartySettings.load(configDirectory.resolve("config.yml"));
        PartyService parties = new PartyService(
                new PropertiesPartyStore(dataRoot.resolve("parties.properties")),
                partySettings,
                new ProgressionFormula(progressionSettings.curve()),
                progressionSettings.mode(),
                progressionSettings.formulaType());
        ChatSettings chatSettings = ChatSettings.load(configDirectory.resolve("chat.yml"));
        ItemWeightSettings itemWeights = ItemWeightSettings.load(configDirectory.resolve("itemweights.yml"));
        ChatStateService chats = new ChatStateService();
        CommandPermissionService permissions = new FabricCommandPermissionService();
        PlayerVisibilityService visibility = new PlayerVisibilityService();
        PartyGameplayService partyGameplay = new PartyGameplayService(
                server, parties, itemWeights, visibility, new Random());
        PartyTeleportService teleports = new PartyTeleportService(
                server, parties, permissions, api.protection(), visibility, Clock.systemUTC());

        Map<io.github.njw3995.fabricmmo.api.NamespacedId, Double> skillBaselines = new HashMap<>();
        CoreSkills.primarySkillIds().forEach(id ->
                skillBaselines.put(id, progressionSettings.globalXpMultiplier()));
        XpRateService xpRates = new XpRateService(
                progressionSettings.globalXpMultiplier(), skillBaselines, Clock.systemUTC());
        PlayerSessionStateService sessions = new PlayerSessionStateService();
        PlayerUiSettingsService uiSettings = new PlayerUiSettingsService(
                new InMemoryPlayerUiSettingsStore());
        Path configFile = configDirectory.resolve("config.yml");
        FlatYamlConfig advancedConfiguration = FlatYamlConfig.load(
                configDirectory.resolve("advanced.yml"));
        UiSettings uiConfiguration = UiSettings.load(
                configFile, configDirectory.resolve("experience.yml"));
        boolean skillCommandBlankLines = advancedConfiguration.bool(
                "Feedback.SkillCommand.BlankLinesAboveHeader", true);
        boolean uiTraceEnabled = FlatYamlConfig.load(configFile)
                .bool("Debugging.UI_Trace.Enabled", false);
        LocaleService locale = LocaleService.loadDefault();
        ScoreboardTipService scoreboardTips = new ScoreboardTipService(
                dataRoot.resolve("scoreboard-tips.properties"),
                uiConfiguration.tipsAmount(),
                locale);
        PlayerScoreboardService scoreboards = new PlayerScoreboardService(
                scoreboardTips::timedBoardShown, uiConfiguration.rainbows());
        XpBossBarService xpBars = new XpBossBarService(
                api, locale, progressionSettings, uiConfiguration);
        AbilityCooldownService cooldowns = cooldowns(
                miningAbilities,
                miningSettings,
                woodcuttingAbilities,
                woodcuttingSettings,
                excavationAbilities,
                excavationSettings,
                herbalismAbilities,
                herbalismSettings,
                swordsAbilities,
                swordsSettings);
        SkillPanelCooldownCatalog skillPanelCooldowns = new SkillPanelCooldownCatalog(
                cooldowns, locale, uiConfiguration.abilityNames());
        SubSkillRankCatalog subSkillRanks = SubSkillRankCatalog.load(
                configDirectory.resolve("skillranks.yml"), progressionSettings.mode());
        SkillPanelMechanicsCatalog skillPanelMechanics = new SkillPanelMechanicsCatalog();
        skillPanelMechanics.register(
                CoreSkills.ACROBATICS,
                new AcrobaticsPanelMechanicsProvider(server, acrobaticsSettings, locale));
        skillPanelMechanics.register(
                CoreSkills.MINING,
                new MiningPanelMechanicsProvider(server, miningSettings, miningDropSettings, locale));
        skillPanelMechanics.register(
                CoreSkills.WOODCUTTING,
                new WoodcuttingPanelMechanicsProvider(
                        server, woodcuttingSettings, woodcuttingDropSettings, locale));
        skillPanelMechanics.register(
                CoreSkills.EXCAVATION,
                new ExcavationPanelMechanicsProvider(
                        server, excavationAbilities, excavationSettings, locale));
        skillPanelMechanics.register(
                CoreSkills.HERBALISM,
                new HerbalismPanelMechanicsProvider(server, herbalismSettings, locale));
        skillPanelMechanics.register(
                CoreSkills.FISHING,
                new FishingPanelMechanicsProvider(server, fishingSettings, fishingTreasures, locale));
        skillPanelMechanics.register(
                CoreSkills.SWORDS,
                new SwordsPanelMechanicsProvider(server, swordsSettings, locale));
        skillPanelMechanics.register(
                CoreSkills.TAMING,
                new TamingPanelMechanicsProvider(tamingSettings, locale));
        skillPanelMechanics.register(
                CoreSkills.ALCHEMY,
                new AlchemyPanelMechanicsProvider(server, alchemySettings, locale));
        DebugDiagnosticsService diagnostics = new DebugDiagnosticsService(server, sessions);
        ProgressionMaintenanceService maintenance = new ProgressionMaintenanceService(
                progressionStore, playerDataDirectory, mySqlSettings, Clock.systemUTC());
        ScheduledMaintenanceService scheduledMaintenance = ScheduledMaintenanceService.load(
                configDirectory.resolve("config.yml"), maintenance, parties, Clock.systemUTC(),
                message -> LOGGER.info(message));

        State newState = new State(
                server,
                worldRoot,
                playerDataDirectory,
                configDirectory,
                api,
                identities,
                parties,
                partyGameplay,
                chatSettings,
                chats,
                teleports,
                visibility,
                permissions,
                xpRates,
                sessions,
                uiSettings,
                uiConfiguration,
                scoreboards,
                scoreboardTips,
                xpBars,
                cooldowns,
                diagnostics,
                new ProgressionAdminService(api.progression(), api.skillRegistry()),
                new LeaderboardService(
                        progressionStore,
                        identities,
                        CoreSkills.primarySkillIds().stream().sorted().toList()),
                locale,
                new SkillGuideCatalog(locale),
                new SkillPanelService(
                        api,
                        skillPanelMechanics,
                        subSkillRanks,
                        permissions,
                        skillPanelCooldowns,
                        locale,
                        uiConfiguration.rainbows(),
                        skillCommandBlankLines),
                new ExperienceConversionService(
                        progressionStore,
                        progressionSettings,
                        dataRoot.resolve("formula.properties"),
                        playerDataDirectory,
                        Clock.systemUTC()),
                new LegacyFlatFileImporter(progressionStore),
                maintenance,
                scheduledMaintenance,
                new ArrayList<>());
        state = newState;
        UiTraceLogger.configure(uiTraceEnabled);
        UiTraceLogger.configuration(
                uiConfiguration.scoreboardsEnabled(),
                uiConfiguration.rainbows(),
                uiConfiguration.abilityNames(),
                skillCommandBlankLines);
        PartyXpRuntime.install(partyGameplay::distributeXp);
        XpRateRuntime.install(xpRates);
        registerEvents(newState, partySettings);
        LOGGER.info("Started FabricMMO shared command, UI, party, leaderboard, diagnostics, and XP-rate services");
    }

    private static void backfillIdentities(
            MinecraftServer server,
            ManagedProgressionStore progressionStore,
            PlayerIdentityStore identities) throws IOException {
        for (UUID playerId : progressionStore.playerIds()) {
            if (identities.findName(playerId).isPresent()) {
                continue;
            }
            server.getUserCache().getByUuid(playerId).ifPresent(profile -> {
                String name = profile.getName();
                if (name != null && !name.isBlank()) {
                    identities.remember(playerId, name);
                }
            });
        }
    }

    private static AbilityCooldownService cooldowns(
            MiningAbilityController miningAbilities,
            MiningSettings miningSettings,
            WoodcuttingAbilityController woodcuttingAbilities,
            WoodcuttingSettings woodcuttingSettings,
            ExcavationAbilityController excavationAbilities,
            ExcavationSettings excavationSettings,
            HerbalismAbilityController herbalismAbilities,
            HerbalismSettings herbalismSettings,
            SwordsAbilityController swordsAbilities,
            SwordsSettings swordsSettings) {
        AbilityCooldownService cooldowns = new AbilityCooldownService();
        AbilityCooldownService.Provider miningProvider = new AbilityCooldownService.Provider() {
            @Override
            public int remainingSeconds(UUID playerId) {
                try {
                    return miningAbilities.superBreakerCooldownRemaining(playerId, miningSettings);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }

            @Override
            public void reset(UUID playerId) {
                try {
                    miningAbilities.reset(playerId);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }
        };
        cooldowns.register(CoreMiningAbilities.SUPER_BREAKER, miningProvider);
        cooldowns.register(CoreMiningAbilities.BLAST_MINING, new AbilityCooldownService.Provider() {
            @Override
            public int remainingSeconds(UUID playerId) {
                try {
                    return miningAbilities.blastCooldownRemaining(playerId, miningSettings);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }

            @Override
            public void reset(UUID playerId) {
                miningProvider.reset(playerId);
            }
        });
        cooldowns.register(CoreWoodcuttingAbilities.TREE_FELLER,
                new AbilityCooldownService.Provider() {
                    @Override
                    public int remainingSeconds(UUID playerId) {
                        try {
                            return woodcuttingAbilities.cooldownRemaining(
                                    playerId, woodcuttingSettings.treeFellerCooldownSeconds());
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    }

                    @Override
                    public void reset(UUID playerId) {
                        try {
                            woodcuttingAbilities.reset(playerId);
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    }
                });
        cooldowns.register(CoreExcavationAbilities.GIGA_DRILL_BREAKER,
                new AbilityCooldownService.Provider() {
                    @Override
                    public int remainingSeconds(UUID playerId) {
                        try {
                            return excavationAbilities.cooldownRemaining(
                                    playerId, excavationSettings.gigaDrillCooldownSeconds());
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    }

                    @Override
                    public void reset(UUID playerId) {
                        try {
                            excavationAbilities.reset(playerId);
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    }
                });
        cooldowns.register(CoreHerbalismAbilities.GREEN_TERRA,
                new AbilityCooldownService.Provider() {
                    @Override
                    public int remainingSeconds(UUID playerId) {
                        try {
                            return herbalismAbilities.cooldownRemaining(
                                    playerId, herbalismSettings.greenTerraCooldownSeconds());
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    }

                    @Override
                    public void reset(UUID playerId) {
                        try {
                            herbalismAbilities.reset(playerId);
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    }
                });
        cooldowns.register(CoreSwordsAbilities.SERRATED_STRIKES,
                new AbilityCooldownService.Provider() {
                    @Override
                    public int remainingSeconds(UUID playerId) {
                        try {
                            return swordsAbilities.cooldownRemaining(
                                    playerId, swordsSettings.serratedCooldownSeconds());
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    }

                    @Override
                    public void reset(UUID playerId) {
                        try {
                            swordsAbilities.reset(playerId);
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    }
                });
        return cooldowns;
    }

    private static void registerEvents(State current, PartySettings partySettings) {
        current.subscriptions().add(current.api().events().subscribe(
                XpPreAwardEvent.class, current.diagnostics()::preAward));
        current.subscriptions().add(current.api().events().subscribe(
                AbilityStateEvent.class, current.diagnostics()::ability));
        current.subscriptions().add(current.api().events().subscribe(LevelChangedEvent.class, event -> {
            current.diagnostics().levelChanged(event);
            ServerPlayerEntity player = current.server().getPlayerManager().getPlayer(event.playerId());
            if (player != null && event.newLevel() > event.oldLevel()
                    && partySettings.levelUpSounds()
                    && current.sessions().get(player.getUuid()).levelUpSound()) {
                player.getWorld().playSound(
                        null,
                        player.getBlockPos(),
                        SoundEvents.ENTITY_PLAYER_LEVELUP,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F);
            }
        }));
        current.subscriptions().add(current.api().events().subscribe(XpAwardedEvent.class, event -> {
            current.diagnostics().awarded(event);
            ServerPlayerEntity player = current.server().getPlayerManager()
                    .getPlayer(event.request().playerId());
            if (event.result().status() == io.github.njw3995.fabricmmo.api.progression.XpAwardResult.Status.APPLIED
                    && !"COMMAND".equalsIgnoreCase(event.request().context().get("upstreamReason"))) {
                var partyResult = current.partyGameplay().recordPartyXp(
                        event.request().playerId(), event.result().appliedXp());
                if (partyResult.levelsGained() > 0 && partyResult.party().isPresent()) {
                    var party = partyResult.party().orElseThrow();
                    Text message = io.github.njw3995.fabricmmo.core.command.LegacyText.parse(
                            current.locale().text(
                                    "Party.LevelUp", partyResult.levelsGained(), partyResult.newLevel()));
                    if (partySettings.informAllMembersOnLevelUp()) {
                        for (UUID memberId : party.members()) {
                            ServerPlayerEntity member = current.server().getPlayerManager().getPlayer(memberId);
                            if (member != null) {
                                member.sendMessage(message);
                            }
                        }
                    } else if (player != null) {
                        player.sendMessage(message);
                    }
                }
            }
            if (player == null || !current.sessions().get(player.getUuid()).notifications()
                    || !current.uiConfiguration().xpBarsEnabled()) {
                return;
            }
            XpBarMode mode = current.uiSettings().get(player.getUuid())
                    .xpBar(event.request().skillId());
            UiSettings.XpBar barSettings = current.uiConfiguration().xpBar(event.request().skillId());
            boolean partyGain = Boolean.parseBoolean(event.request().context().getOrDefault(
                    "fabricmmo.party.shared", "false"));
            boolean passiveGain = Boolean.parseBoolean(event.request().context().getOrDefault(
                    "fabricmmo.passive", "false"));
            if (mode == XpBarMode.HIDDEN || !barSettings.enabled()
                    || (partyGain && !current.uiConfiguration().updatePartyXp())
                    || (passiveGain && !current.uiConfiguration().updatePassiveXp())) {
                return;
            }
            current.xpBars().show(
                    player,
                    current.api().progression().query(
                            player.getUuid(), event.request().skillId()),
                    barSettings, mode, current.server().getTicks());
        }));
    }

    public static synchronized State require() {
        if (state == null) {
            throw new IllegalStateException("FabricMMO shared server systems are not active");
        }
        return state;
    }

    public static synchronized boolean running() {
        return state != null;
    }

    public static synchronized boolean godMode(UUID id) {
        return state != null && state.sessions().get(id).godMode();
    }

    public static boolean sharePartyItem(ServerPlayerEntity player, ItemEntity item) {
        State current;
        synchronized (SharedServerSystems.class) {
            current = state;
        }
        return current != null && current.partyGameplay().shareItem(player, item);
    }

    public static void playerJoined(ServerPlayerEntity player) {
        State current = require();
        current.identities().remember(player.getUuid(), player.getGameProfile().getName());
        try {
            current.maintenance().touch(player.getUuid());
        } catch (IOException exception) {
            LOGGER.warn("Unable to update last-seen timestamp for {}", player.getUuid(), exception);
        }
        if (current.chatSettings().automaticPartySpy()
                && current.permissions().hasPermission(
                player.getCommandSource(), "mcmmo.commands.mcchatspy", false)
                && !current.chats().spying(player.getUuid())) {
            current.chats().toggleSpy(player.getUuid());
        }
        if (current.uiConfiguration().scoreboardsEnabled()
                && current.uiConfiguration().showStatsAfterLogin()
                && current.uiConfiguration().board(UiSettings.BoardType.STATS).enabled()) {
            List<Text> lines = StatsTextFormatter.format(
                    current.api().skillRegistry(),
                    current.api().progression().queryAll(player.getUuid()));
            List<Text> boardLines = lines.size() > 2
                    ? lines.subList(2, Math.min(lines.size(), 17))
                    : lines;
            current.scoreboards().show(
                    player, Text.literal("mcMMO Stats"), boardLines,
                    current.uiConfiguration().board(UiSettings.BoardType.STATS).displaySeconds());
        }
        var rates = current.xpRates().snapshot();
        if (rates.active()) {
            player.sendMessage(Text.literal(
                    "FabricMMO XP rates are active. Use /xprate show for details."));
        }
    }

    public static void playerDisconnected(ServerPlayerEntity player) {
        State current;
        synchronized (SharedServerSystems.class) {
            current = state;
        }
        if (current == null) {
            return;
        }
        current.identities().remember(player.getUuid(), player.getGameProfile().getName());
        current.sessions().remove(player.getUuid());
        current.uiSettings().remove(player.getUuid());
        current.chats().remove(player.getUuid());
        current.teleports().remove(player.getUuid());
        current.partyGameplay().remove(player.getUuid());
        current.scoreboards().remove(player);
        current.scoreboardTips().remove(player.getUuid());
        current.xpBars().remove(player.getUuid());
    }

    public static void playerHurt(ServerPlayerEntity player) {
        State current;
        synchronized (SharedServerSystems.class) {
            current = state;
        }
        if (current != null) {
            current.teleports().markHurt(player.getUuid());
        }
    }

    public static void tick(MinecraftServer server) {
        State current;
        synchronized (SharedServerSystems.class) {
            current = state;
        }
        if (current == null) {
            return;
        }
        current.teleports().tick();
        current.scoreboards().tick(server);
        current.xpBars().tick(server);
        current.scheduledMaintenance().tick(server);
    }

    public static synchronized void stop() {
        State current = state;
        state = null;
        XpRateRuntime.clear();
        PartyXpRuntime.clear();
        UiTraceLogger.clear();
        if (current == null) {
            return;
        }
        current.subscriptions().forEach(EventSubscription::close);
        current.scoreboards().clearAll();
        current.scoreboardTips().clearSessions();
        current.xpBars().clear();
        current.teleports().clear();
        current.partyGameplay().clear();
        current.sessions().clear();
        current.uiSettings().clear();
        current.chats().clear();
        LOGGER.info("Stopped FabricMMO shared server systems");
    }

    public record State(
            MinecraftServer server,
            Path worldRoot,
            Path playerDataDirectory,
            Path configDirectory,
            FabricMmoApi api,
            PlayerIdentityStore identities,
            PartyService parties,
            PartyGameplayService partyGameplay,
            ChatSettings chatSettings,
            ChatStateService chats,
            PartyTeleportService teleports,
            PlayerVisibilityService visibility,
            CommandPermissionService permissions,
            XpRateService xpRates,
            PlayerSessionStateService sessions,
            PlayerUiSettingsService uiSettings,
            UiSettings uiConfiguration,
            PlayerScoreboardService scoreboards,
            ScoreboardTipService scoreboardTips,
            XpBossBarService xpBars,
            AbilityCooldownService cooldowns,
            DebugDiagnosticsService diagnostics,
            ProgressionAdminService progressionAdmin,
            LeaderboardService leaderboards,
            LocaleService locale,
            SkillGuideCatalog guides,
            SkillPanelService skillPanels,
            ExperienceConversionService conversion,
            LegacyFlatFileImporter importer,
            ProgressionMaintenanceService maintenance,
            ScheduledMaintenanceService scheduledMaintenance,
            List<EventSubscription> subscriptions) {
    }
}

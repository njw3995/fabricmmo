package io.github.njw3995.fabricmmo.core.bootstrap;

import io.github.njw3995.fabricmmo.api.ApiVersion;
import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.AbilityService;
import io.github.njw3995.fabricmmo.api.ability.PassiveService;
import io.github.njw3995.fabricmmo.api.ability.AbilityStateRegistrar;
import io.github.njw3995.fabricmmo.api.ability.AbilityStateView;
import io.github.njw3995.fabricmmo.api.command.CommandMetadataRegistrar;
import io.github.njw3995.fabricmmo.api.command.CommandMetadataRegistryView;
import io.github.njw3995.fabricmmo.api.config.ConfigRegistrar;
import io.github.njw3995.fabricmmo.api.config.ConfigService;
import io.github.njw3995.fabricmmo.api.config.ConfigRegistryView;
import io.github.njw3995.fabricmmo.api.content.BrewingContentRegistrar;
import io.github.njw3995.fabricmmo.api.content.BrewingContentRegistryView;
import io.github.njw3995.fabricmmo.api.content.EntityXpContentRegistrar;
import io.github.njw3995.fabricmmo.api.content.EntityXpContentRegistryView;
import io.github.njw3995.fabricmmo.api.content.GatheringContentRegistrar;
import io.github.njw3995.fabricmmo.api.content.GatheringContentRegistryView;
import io.github.njw3995.fabricmmo.api.event.FabricMmoEventBus;
import io.github.njw3995.fabricmmo.api.data.PersistentMarkerService;
import io.github.njw3995.fabricmmo.api.progression.GameplayXpService;
import io.github.njw3995.fabricmmo.api.progression.ProgressionService;
import io.github.njw3995.fabricmmo.api.progression.XpSourceRegistrar;
import io.github.njw3995.fabricmmo.api.progression.XpSourceRegistryView;
import io.github.njw3995.fabricmmo.api.protection.ProtectionProviderRegistrar;
import io.github.njw3995.fabricmmo.api.protection.ProtectionService;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistrar;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.api.ui.UiMetadataRegistrar;
import io.github.njw3995.fabricmmo.api.ui.UiMetadataRegistryView;
import io.github.njw3995.fabricmmo.core.ability.AbilityPipeline;
import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.ability.PassivePipeline;
import io.github.njw3995.fabricmmo.core.command.DefaultCommandMetadataRegistry;
import io.github.njw3995.fabricmmo.core.config.DefaultConfigRegistry;
import io.github.njw3995.fabricmmo.core.content.DefaultBrewingContentRegistry;
import io.github.njw3995.fabricmmo.core.content.DefaultEntityXpContentRegistry;
import io.github.njw3995.fabricmmo.core.content.DefaultGatheringContentRegistry;
import io.github.njw3995.fabricmmo.core.data.DefaultPersistentMarkerService;
import io.github.njw3995.fabricmmo.core.progression.DefaultGameplayXpService;
import io.github.njw3995.fabricmmo.core.progression.DefaultXpSourceRegistry;
import io.github.njw3995.fabricmmo.core.protection.CompositeProtectionService;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.ui.DefaultUiMetadataRegistry;

public record DefaultFabricMmoApi(
        DefaultSkillRegistry skills,
        DefaultXpSourceRegistry xpSourceRegistry,
        DefaultAbilityRegistry abilityRegistry,
        AbilityPipeline abilityPipeline,
        PassivePipeline passivePipeline,
        DefaultGameplayXpService gameplayXpService,
        DefaultConfigRegistry configs,
        DefaultGatheringContentRegistry gatheringContentRegistry,
        DefaultEntityXpContentRegistry entityXpContentRegistry,
        DefaultBrewingContentRegistry brewingContentRegistry,
        DefaultCommandMetadataRegistry commands,
        DefaultUiMetadataRegistry ui,
        FabricMmoEventBus events,
        ProgressionService progression,
        CompositeProtectionService protection,
        DefaultPersistentMarkerService markers) implements FabricMmoApi {

    @Override
    public String apiVersion() {
        return ApiVersion.STRING;
    }

    @Override
    public SkillRegistrar skillRegistrar() {
        return skills;
    }

    @Override
    public SkillRegistryView skillRegistry() {
        return skills;
    }

    @Override
    public XpSourceRegistrar xpSourceRegistrar() {
        return xpSourceRegistry;
    }

    @Override
    public XpSourceRegistryView xpSources() {
        return xpSourceRegistry;
    }

    @Override
    public AbilityRegistrar abilityRegistrar() {
        return abilityRegistry;
    }

    @Override
    public AbilityStateView abilityStates() {
        return abilityPipeline;
    }

    @Override
    public AbilityService abilities() {
        return abilityPipeline;
    }

    @Override
    public PassiveService passives() {
        return passivePipeline;
    }

    @Override
    public AbilityStateRegistrar abilityStateRegistrar() {
        return abilityPipeline;
    }

    @Override
    public GameplayXpService gameplayXp() {
        return gameplayXpService;
    }

    @Override
    public ConfigRegistrar configRegistrar() {
        return configs;
    }

    @Override
    public ConfigRegistryView configRegistry() {
        return configs;
    }

    @Override
    public ConfigService config() {
        return configs;
    }

    @Override
    public GatheringContentRegistrar gatheringContentRegistrar() {
        return gatheringContentRegistry;
    }

    @Override
    public GatheringContentRegistryView gatheringContent() {
        return gatheringContentRegistry;
    }

    @Override
    public EntityXpContentRegistrar entityXpContentRegistrar() {
        return entityXpContentRegistry;
    }

    @Override
    public EntityXpContentRegistryView entityXpContent() {
        return entityXpContentRegistry;
    }

    @Override
    public BrewingContentRegistrar brewingContentRegistrar() {
        return brewingContentRegistry;
    }

    @Override
    public BrewingContentRegistryView brewingContent() {
        return brewingContentRegistry;
    }

    @Override
    public CommandMetadataRegistrar commandMetadataRegistrar() {
        return commands;
    }

    @Override
    public CommandMetadataRegistryView commandMetadata() {
        return commands;
    }

    @Override
    public UiMetadataRegistrar uiMetadataRegistrar() {
        return ui;
    }

    @Override
    public UiMetadataRegistryView uiMetadata() {
        return ui;
    }

    @Override
    public ProtectionProviderRegistrar protectionProviderRegistrar() {
        return protection;
    }

    @Override
    public PersistentMarkerService persistentMarkers() {
        return markers;
    }
}

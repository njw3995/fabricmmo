package io.github.njw3995.fabricmmo.core.bootstrap;

import io.github.njw3995.fabricmmo.api.ApiVersion;
import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.AbilityStateView;
import io.github.njw3995.fabricmmo.api.command.CommandMetadataRegistrar;
import io.github.njw3995.fabricmmo.api.command.CommandMetadataRegistryView;
import io.github.njw3995.fabricmmo.api.config.ConfigRegistrar;
import io.github.njw3995.fabricmmo.api.config.ConfigRegistryView;
import io.github.njw3995.fabricmmo.api.event.FabricMmoEventBus;
import io.github.njw3995.fabricmmo.api.progression.ProgressionService;
import io.github.njw3995.fabricmmo.api.progression.XpSourceRegistrar;
import io.github.njw3995.fabricmmo.api.progression.XpSourceRegistryView;
import io.github.njw3995.fabricmmo.api.protection.ProtectionService;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistrar;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.api.ui.UiMetadataRegistrar;
import io.github.njw3995.fabricmmo.api.ui.UiMetadataRegistryView;
import io.github.njw3995.fabricmmo.core.ability.AbilityPipeline;
import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.command.DefaultCommandMetadataRegistry;
import io.github.njw3995.fabricmmo.core.config.DefaultConfigRegistry;
import io.github.njw3995.fabricmmo.core.progression.DefaultXpSourceRegistry;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.ui.DefaultUiMetadataRegistry;

public record DefaultFabricMmoApi(
        DefaultSkillRegistry skills,
        DefaultXpSourceRegistry xpSourceRegistry,
        DefaultAbilityRegistry abilities,
        AbilityPipeline abilityPipeline,
        DefaultConfigRegistry configs,
        DefaultCommandMetadataRegistry commands,
        DefaultUiMetadataRegistry ui,
        FabricMmoEventBus events,
        ProgressionService progression,
        ProtectionService protection) implements FabricMmoApi {

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
        return abilities;
    }

    @Override
    public AbilityStateView abilityStates() {
        return abilityPipeline;
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
}

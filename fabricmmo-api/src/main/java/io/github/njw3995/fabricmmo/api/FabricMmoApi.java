package io.github.njw3995.fabricmmo.api;

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

public interface FabricMmoApi {
    String apiVersion();

    SkillRegistrar skillRegistrar();

    SkillRegistryView skillRegistry();

    XpSourceRegistrar xpSourceRegistrar();

    XpSourceRegistryView xpSources();

    AbilityRegistrar abilityRegistrar();

    AbilityStateView abilityStates();

    ConfigRegistrar configRegistrar();

    ConfigRegistryView configRegistry();

    CommandMetadataRegistrar commandMetadataRegistrar();

    CommandMetadataRegistryView commandMetadata();

    UiMetadataRegistrar uiMetadataRegistrar();

    UiMetadataRegistryView uiMetadata();

    FabricMmoEventBus events();

    ProgressionService progression();

    ProtectionService protection();
}

package io.github.njw3995.fabricmmo.core.bootstrap;

import io.github.njw3995.fabricmmo.api.protection.ProtectionService;
import io.github.njw3995.fabricmmo.core.ability.AbilityPipeline;
import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.command.CoreCommandMetadata;
import io.github.njw3995.fabricmmo.core.command.DefaultCommandMetadataRegistry;
import io.github.njw3995.fabricmmo.core.config.DefaultConfigRegistry;
import io.github.njw3995.fabricmmo.core.event.SimpleEventBus;
import io.github.njw3995.fabricmmo.core.persistence.ProgressionStore;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.progression.DefaultProgressionService;
import io.github.njw3995.fabricmmo.core.progression.DefaultXpSourceRegistry;
import io.github.njw3995.fabricmmo.core.progression.ProgressionFormula;
import io.github.njw3995.fabricmmo.core.progression.ProgressionSettings;
import io.github.njw3995.fabricmmo.core.protection.AllowAllProtectionService;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import io.github.njw3995.fabricmmo.core.skill.acrobatics.CoreAcrobaticsAbilities;
import io.github.njw3995.fabricmmo.core.skill.archery.CoreArcheryAbilities;
import io.github.njw3995.fabricmmo.core.skill.axes.CoreAxesAbilities;
import io.github.njw3995.fabricmmo.core.skill.crossbows.CoreCrossbowsAbilities;
import io.github.njw3995.fabricmmo.core.skill.alchemy.CoreAlchemyAbilities;
import io.github.njw3995.fabricmmo.core.skill.excavation.CoreExcavationAbilities;
import io.github.njw3995.fabricmmo.core.skill.herbalism.CoreHerbalismAbilities;
import io.github.njw3995.fabricmmo.core.skill.fishing.CoreFishingAbilities;
import io.github.njw3995.fabricmmo.core.skill.maces.CoreMacesAbilities;
import io.github.njw3995.fabricmmo.core.skill.mining.CoreMiningAbilities;
import io.github.njw3995.fabricmmo.core.skill.swords.CoreSwordsAbilities;
import io.github.njw3995.fabricmmo.core.skill.tridents.CoreTridentsAbilities;
import io.github.njw3995.fabricmmo.core.skill.unarmed.CoreUnarmedAbilities;
import io.github.njw3995.fabricmmo.core.skill.taming.CoreTamingAbilities;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.CoreWoodcuttingAbilities;
import io.github.njw3995.fabricmmo.core.ui.DefaultUiMetadataRegistry;
import java.time.Clock;
import java.util.Objects;
import java.util.function.Consumer;

public final class FabricMmoBootstrap {
    private FabricMmoBootstrap() {
    }

    public static DefaultFabricMmoApi create(
            ProgressionStore store,
            Consumer<DefaultFabricMmoApi> addonRegistration) {
        return create(store, new AllowAllProtectionService(), Clock.systemUTC(),
                ProgressionSettings.upstreamDefaults(), addonRegistration);
    }

    public static DefaultFabricMmoApi create(
            ProgressionStore store,
            ProtectionService protection,
            Clock clock,
            Consumer<DefaultFabricMmoApi> addonRegistration) {
        return create(store, protection, clock, ProgressionSettings.upstreamDefaults(),
                addonRegistration);
    }

    public static DefaultFabricMmoApi create(
            ProgressionStore store,
            ProtectionService protection,
            Clock clock,
            ProgressionSettings progressionSettings,
            Consumer<DefaultFabricMmoApi> addonRegistration) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(protection, "protection");
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(progressionSettings, "progressionSettings");
        Objects.requireNonNull(addonRegistration, "addonRegistration");

        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills, progressionSettings);
        SimpleEventBus events = new SimpleEventBus();
        DefaultXpSourceRegistry xpSources = new DefaultXpSourceRegistry(skills);
        CoreXpSources.registerDefaults(xpSources);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);
        CoreAcrobaticsAbilities.registerAll(abilities);
        CoreArcheryAbilities.registerAll(abilities);
        CoreAxesAbilities.registerAll(abilities);
        CoreCrossbowsAbilities.registerAll(abilities);
        CoreMacesAbilities.registerAll(abilities);
        CoreAlchemyAbilities.registerAll(abilities);
        CoreMiningAbilities.registerAll(abilities);
        CoreWoodcuttingAbilities.registerAll(abilities);
        CoreExcavationAbilities.registerAll(abilities);
        CoreHerbalismAbilities.registerAll(abilities);
        CoreFishingAbilities.registerAll(abilities);
        CoreSwordsAbilities.registerAll(abilities);
        CoreTridentsAbilities.registerAll(abilities);
        CoreUnarmedAbilities.registerAll(abilities);
        CoreTamingAbilities.registerAll(abilities);
        AbilityPipeline abilityPipeline = new AbilityPipeline(abilities, events, clock);
        DefaultConfigRegistry configs = new DefaultConfigRegistry();
        DefaultCommandMetadataRegistry commands = new DefaultCommandMetadataRegistry();
        CoreCommandMetadata.registerDefaults(commands);
        DefaultUiMetadataRegistry ui = new DefaultUiMetadataRegistry(skills);
        DefaultProgressionService progression = new DefaultProgressionService(
                skills,
                store,
                new ProgressionFormula(progressionSettings.curve()),
                xpSources,
                events,
                progressionSettings,
                clock);
        DefaultFabricMmoApi api = new DefaultFabricMmoApi(
                skills,
                xpSources,
                abilities,
                abilityPipeline,
                configs,
                commands,
                ui,
                events,
                progression,
                protection);

        addonRegistration.accept(api);
        CoreXpSources.registerCommandSources(skills.skills(), xpSources);

        skills.freeze();
        xpSources.freeze();
        abilities.freeze();
        configs.freeze();
        commands.freeze();
        ui.freeze();
        return api;
    }
}

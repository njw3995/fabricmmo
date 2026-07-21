package io.github.njw3995.fabricmmo.api;

import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.AbilityService;
import io.github.njw3995.fabricmmo.api.ability.AbilityStateRegistrar;
import io.github.njw3995.fabricmmo.api.ability.AbilityStateView;
import io.github.njw3995.fabricmmo.api.ability.PassiveService;
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

public interface FabricMmoApi {
    String apiVersion();

    SkillRegistrar skillRegistrar();

    SkillRegistryView skillRegistry();

    XpSourceRegistrar xpSourceRegistrar();

    XpSourceRegistryView xpSources();

    AbilityRegistrar abilityRegistrar();

    AbilityStateView abilityStates();

    /** Core-owned preparation, activation, duration, and cooldown state for addon abilities. */
    default AbilityService abilities() {
        return AbilityService.unsupported();
    }

    /** Resolves registered passive chances through unlock checks and cancellable events. */
    default PassiveService passives() {
        return PassiveService.unsupported();
    }

    /** Registers addon-owned state for an active ability declared through {@link #abilityRegistrar()}. */
    default AbilityStateRegistrar abilityStateRegistrar() {
        return AbilityStateRegistrar.unsupported();
    }

    ConfigRegistrar configRegistrar();

    ConfigRegistryView configRegistry();

    /** Materializes and reads defaults registered through {@link #configRegistrar()}. */
    default ConfigService config() {
        return ConfigService.unsupported();
    }

    /** Registers blocks or block tags with core-owned gathering mechanics. */
    default GatheringContentRegistrar gatheringContentRegistrar() {
        return GatheringContentRegistrar.unsupported();
    }

    /** Reads gathering declarations registered before the server registries freeze. */
    default GatheringContentRegistryView gatheringContent() {
        return GatheringContentRegistryView.unsupported();
    }

    /** Registers modded entity IDs or tags with core Combat or Taming XP. */
    default EntityXpContentRegistrar entityXpContentRegistrar() {
        return EntityXpContentRegistrar.unsupported();
    }

    /** Reads merged Java and datapack entity XP declarations. */
    default EntityXpContentRegistryView entityXpContent() {
        return EntityXpContentRegistryView.unsupported();
    }

    /** Registers external server-side brewing transformations with core Alchemy. */
    default BrewingContentRegistrar brewingContentRegistrar() {
        return BrewingContentRegistrar.unsupported();
    }

    /** Reads external brewing declarations registered during addon initialization. */
    default BrewingContentRegistryView brewingContent() {
        return BrewingContentRegistryView.unsupported();
    }

    CommandMetadataRegistrar commandMetadataRegistrar();

    CommandMetadataRegistryView commandMetadata();

    UiMetadataRegistrar uiMetadataRegistrar();

    UiMetadataRegistryView uiMetadata();

    FabricMmoEventBus events();

    ProgressionService progression();

    /**
     * Preferred XP entry point for normal external gameplay listeners. Unlike the low-level
     * progression service, this applies online eligibility, permissions, XP perks, and dynamic
     * permission-aware Power Level context before awarding XP.
     */
    default GameplayXpService gameplayXp() {
        return GameplayXpService.unsupported();
    }

    ProtectionService protection();

    /** Registers a generic claims/protection adapter during addon registration. */
    default ProtectionProviderRegistrar protectionProviderRegistrar() {
        return ProtectionProviderRegistrar.unsupported();
    }

    /** Namespaced persistent milestone and replay markers owned and flushed by core. */
    default PersistentMarkerService persistentMarkers() {
        return PersistentMarkerService.unsupported();
    }
}

package io.github.njw3995.fabricmmo.testaddon;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.api.command.CommandMetadata;
import io.github.njw3995.fabricmmo.api.config.ConfigContribution;
import io.github.njw3995.fabricmmo.api.content.BrewingContentDefinition;
import io.github.njw3995.fabricmmo.api.content.ContentSelector;
import io.github.njw3995.fabricmmo.api.content.GatheringContentDefinition;
import io.github.njw3995.fabricmmo.api.content.EntityXpContentDefinition;
import io.github.njw3995.fabricmmo.api.content.MaturityRequirement;
import io.github.njw3995.fabricmmo.api.entrypoint.FabricMmoEntrypoint;
import io.github.njw3995.fabricmmo.api.event.LevelChangedEvent;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpSourceDefinition;
import io.github.njw3995.fabricmmo.api.protection.ProtectionService;
import io.github.njw3995.fabricmmo.api.skill.SkillCategory;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.api.skill.SkillExtension;
import io.github.njw3995.fabricmmo.api.ui.UiMetadata;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;
import java.util.Optional;
import java.util.Set;

public final class TestAddonRegistration implements FabricMmoEntrypoint {
    public static final NamespacedId ENGINEERING = NamespacedId.parse("fabricmmo_test_addon:engineering");
    public static final NamespacedId MINING_EXTENSION = NamespacedId.parse("fabricmmo_test_addon:mining_extension");
    public static final NamespacedId TEST_SOURCE = NamespacedId.parse("fabricmmo_test_addon:test_source");
    public static final NamespacedId OVERCLOCK = NamespacedId.parse("fabricmmo_test_addon:overclock");
    public static final NamespacedId TEST_ORE = NamespacedId.parse("fabricmmo_test_addon:test_ore");
    public static final NamespacedId TEST_BREW = NamespacedId.parse("fabricmmo_test_addon:test_brew");
    public static final NamespacedId TEST_ENTITY_XP = NamespacedId.parse("fabricmmo_test_addon:test_entity_xp");
    public static final NamespacedId TEST_PROTECTION = NamespacedId.parse("fabricmmo_test_addon:test_protection");
    private static final NamespacedId OWNER = NamespacedId.parse("fabricmmo_test_addon:registration");
    private final AtomicInteger observedLevelEvents = new AtomicInteger();

    @Override
    public void register(FabricMmoApi api) {
        api.skillRegistrar().registerSkill(new SkillDefinition(
                ENGINEERING,
                SkillCategory.ADDON,
                "skill.fabricmmo_test_addon.engineering",
                1000,
                true,
                List.of(),
                Map.of("purpose", "public API validation")));
        api.skillRegistrar().registerExtension(new SkillExtension(
                NamespacedId.parse("fabricmmo:mining"),
                MINING_EXTENSION,
                Map.of("purpose", "extend existing skill")));
        api.gatheringContentRegistrar().registerGatheringContent(new GatheringContentDefinition(
                TEST_ORE,
                NamespacedId.parse("fabricmmo:mining"),
                ContentSelector.tag("fabricmmo_test_addon:ores"),
                125,
                Set.of(ContentSelector.tag("minecraft:pickaxes")),
                true,
                MaturityRequirement.any(),
                true,
                true,
                Optional.empty(),
                Map.of("purpose", "external gathering content validation")));
        api.entityXpContentRegistrar().registerEntityXpContent(EntityXpContentDefinition.of(
                TEST_ENTITY_XP,
                EntityXpContentDefinition.Scope.COMBAT,
                ContentSelector.tag("fabricmmo_test_addon:hostiles"),
                3.25D));
        api.brewingContentRegistrar().registerBrewingContent(new BrewingContentDefinition(
                TEST_BREW,
                ContentSelector.id("fabricmmo_test_addon:catalyst"),
                ContentSelector.id("minecraft:glass_bottle"),
                ContentSelector.id("fabricmmo_test_addon:tonic"),
                2,
                Map.of("purpose", "external brewing content validation")));
        api.xpSourceRegistrar().registerXpSource(new XpSourceDefinition(
                TEST_SOURCE,
                ENGINEERING,
                Map.of("purpose", "validated test XP source")));
        api.abilityRegistrar().registerPassive(new PassiveDefinition(
                NamespacedId.parse("fabricmmo_test_addon:careful_measurement"),
                ENGINEERING,
                10,
                Map.of()));
        api.abilityRegistrar().registerActive(new ActiveAbilityDefinition(
                OVERCLOCK,
                ENGINEERING,
                20,
                Duration.ofSeconds(4),
                Duration.ofSeconds(8),
                Duration.ofSeconds(60),
                Map.of()));
        api.abilityStateRegistrar().registerAbilityStateView(
                OVERCLOCK,
                new io.github.njw3995.fabricmmo.api.ability.AbilityStateView() {
                    @Override
                    public boolean isActive(UUID playerId, NamespacedId abilityId) {
                        return OVERCLOCK.equals(abilityId);
                    }

                    @Override
                    public Duration activeRemaining(UUID playerId, NamespacedId abilityId) {
                        return Duration.ofSeconds(8);
                    }

                    @Override
                    public Duration cooldownRemaining(UUID playerId, NamespacedId abilityId) {
                        return Duration.ofSeconds(60);
                    }
                });
        api.protectionProviderRegistrar().registerProtectionProvider(
                TEST_PROTECTION,
                100,
                new ProtectionService() {
                    @Override
                    public boolean canBreak(UUID playerId, String worldId, int x, int y, int z) {
                        return true;
                    }

                    @Override
                    public boolean canModify(UUID playerId, String worldId, int x, int y, int z) {
                        return true;
                    }

                    @Override
                    public boolean canInteract(UUID playerId, String worldId, int x, int y, int z) {
                        return true;
                    }

                    @Override
                    public boolean canDamage(UUID attackerId, UUID targetId, String worldId) {
                        return true;
                    }
                });
        api.configRegistrar().registerConfig(new ConfigContribution(
                OWNER,
                "fabricmmo-test-addon.yml",
                Map.of("Engineering.Enabled", "true")));
        api.commandMetadataRegistrar().registerCommandMetadata(new CommandMetadata(
                NamespacedId.parse("fabricmmo_test_addon:engineering_command"),
                "engineering",
                List.of("engskill"),
                "fabricmmo_test_addon.commands.engineering"));
        api.uiMetadataRegistrar().registerUiMetadata(new UiMetadata(
                ENGINEERING,
                "minecraft:redstone",
                Map.of("category", "addon")));
        api.events().subscribe(LevelChangedEvent.class, ignored -> observedLevelEvents.incrementAndGet());
        api.events().subscribe(MachineCompleted.class, event -> api.progression().award(
                new XpAwardRequest(
                        event.playerId(),
                        ENGINEERING,
                        TEST_SOURCE,
                        event.xp(),
                        Map.of("machine", event.machineId().toString()))));
    }

    /** Example event class entirely owned by an external addon. */
    public record MachineCompleted(UUID playerId, NamespacedId machineId, double xp) {
    }

    public int observedLevelEvents() {
        return observedLevelEvents.get();
    }
}

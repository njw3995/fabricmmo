package io.github.njw3995.fabricmmo.testaddon;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.api.command.CommandMetadata;
import io.github.njw3995.fabricmmo.api.config.ConfigContribution;
import io.github.njw3995.fabricmmo.api.entrypoint.FabricMmoEntrypoint;
import io.github.njw3995.fabricmmo.api.event.LevelChangedEvent;
import io.github.njw3995.fabricmmo.api.progression.XpSourceDefinition;
import io.github.njw3995.fabricmmo.api.skill.SkillCategory;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.api.skill.SkillExtension;
import io.github.njw3995.fabricmmo.api.ui.UiMetadata;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestAddonRegistration implements FabricMmoEntrypoint {
    public static final NamespacedId ENGINEERING = NamespacedId.parse("fabricmmo_test_addon:engineering");
    public static final NamespacedId MINING_EXTENSION = NamespacedId.parse("fabricmmo_test_addon:mining_extension");
    public static final NamespacedId TEST_SOURCE = NamespacedId.parse("fabricmmo_test_addon:test_source");
    public static final NamespacedId OVERCLOCK = NamespacedId.parse("fabricmmo_test_addon:overclock");
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
    }

    public int observedLevelEvents() {
        return observedLevelEvents.get();
    }
}

package io.github.njw3995.fabricmmo.core.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.api.progression.XpSourceDefinition;
import io.github.njw3995.fabricmmo.api.skill.SkillCategory;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.event.SimpleEventBus;
import io.github.njw3995.fabricmmo.core.persistence.InMemoryProgressionStore;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultGameplayXpServiceTest {
    private static final NamespacedId SKILL = NamespacedId.parse("test:engineering");
    private static final NamespacedId SOURCE = NamespacedId.parse("test:machine_completed");

    @Test
    void externalListenerAwardUsesPermissionAndEnrichedContext() {
        Fixture fixture = fixture(permission -> true);
        UUID player = UUID.randomUUID();

        XpAwardResult result = fixture.gameplay().awardOnline(new XpAwardRequest(
                player, SKILL, SOURCE, 100.0D, Map.of("machine", "crusher")));

        assertEquals(XpAwardResult.Status.APPLIED, result.status());
        assertTrue(fixture.progression().query(player, SKILL).xp() > 100);
    }

    @Test
    void rejectsOfflineAndMissingSkillPermissionBeforeProgressionMutation() {
        Fixture denied = fixture(permission -> false);
        UUID player = UUID.randomUUID();
        XpAwardResult missingPermission = denied.gameplay().awardOnline(new XpAwardRequest(
                player, SKILL, SOURCE, 100.0D, Map.of()));
        assertEquals(XpAwardResult.Status.REJECTED, missingPermission.status());
        assertTrue(missingPermission.detail().contains("Missing skill permission"));
        assertEquals(0, denied.progression().query(player, SKILL).xp());

        Fixture offline = fixture(permission -> true, ignored -> Optional.empty());
        XpAwardResult offlineResult = offline.gameplay().awardOnline(new XpAwardRequest(
                player, SKILL, SOURCE, 100.0D, Map.of()));
        assertEquals(XpAwardResult.Status.REJECTED, offlineResult.status());
        assertTrue(offlineResult.detail().contains("not online"));
    }


    @Test
    void addonOwnedEventClassCanDriveValidatedGameplayXpWithoutCoreEventChanges() {
        Fixture fixture = fixture(permission -> true);
        UUID player = UUID.randomUUID();
        fixture.events().subscribe(MachineCompleted.class, event ->
                fixture.gameplay().awardOnline(new XpAwardRequest(
                        event.playerId(),
                        SKILL,
                        SOURCE,
                        event.xp(),
                        Map.of("machine", event.machineId().toString()))));

        fixture.events().publish(new MachineCompleted(
                player,
                NamespacedId.parse("test:crusher"),
                100.0D));

        assertTrue(fixture.progression().query(player, SKILL).xp() > 100);
    }

    private static Fixture fixture(java.util.function.Predicate<String> permission) {
        return fixture(permission, ignored -> Optional.of(
                DefaultGameplayXpService.PlayerContext.eligible(
                        permission,
                        (base, skill) -> {
                            java.util.HashMap<String, String> enriched = new java.util.HashMap<>(base);
                            enriched.put("xpPerkMultiplier", "1.5");
                            enriched.put("powerLevelSkills", SKILL.toString());
                            return Map.copyOf(enriched);
                        })));
    }

    private static Fixture fixture(
            java.util.function.Predicate<String> permission,
            DefaultGameplayXpService.PlayerResolver resolver) {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        skills.registerSkill(new SkillDefinition(
                SKILL,
                SkillCategory.ADDON,
                "skill.test.engineering",
                1000,
                true,
                List.of(),
                Map.of("permission", "test.skills.engineering", "power_level", "true")));
        DefaultXpSourceRegistry sources = new DefaultXpSourceRegistry(skills);
        sources.registerXpSource(new XpSourceDefinition(SOURCE, SKILL, Map.of()));
        skills.freeze();
        sources.freeze();
        ProgressionSettings settings = ProgressionSettings.upstreamDefaults();
        SimpleEventBus events = new SimpleEventBus();
        DefaultProgressionService progression = new DefaultProgressionService(
                skills,
                new InMemoryProgressionStore(),
                new ProgressionFormula(settings.curve()),
                sources,
                events,
                settings,
                Clock.systemUTC());
        DefaultGameplayXpService gameplay = new DefaultGameplayXpService(
                skills, sources, progression, settings, resolver);
        return new Fixture(gameplay, progression, events);
    }

    private record Fixture(
            DefaultGameplayXpService gameplay,
            DefaultProgressionService progression,
            SimpleEventBus events) {
    }

    private record MachineCompleted(UUID playerId, NamespacedId machineId, double xp) {
    }
}

package io.github.njw3995.fabricmmo.core.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.core.event.SimpleEventBus;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AbilityPipelineTest {
    @Test
    void preparationAndActivationAreServerAuthoritative() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);
        NamespacedId superBreaker = NamespacedId.parse("fabricmmo:super_breaker");
        abilities.registerActive(new ActiveAbilityDefinition(superBreaker, CoreSkills.MINING, 1,
                Duration.ofSeconds(4), Duration.ofSeconds(6), Duration.ofSeconds(10), Map.of()));
        abilities.freeze();
        AbilityPipeline pipeline = new AbilityPipeline(abilities, new SimpleEventBus(),
                Clock.fixed(Instant.parse("2026-07-16T12:00:00Z"), ZoneOffset.UTC));
        UUID player = UUID.randomUUID();
        assertFalse(pipeline.prepare(player, superBreaker, 0));
        assertTrue(pipeline.prepare(player, superBreaker, 1));
        assertTrue(pipeline.activate(player, superBreaker));
        assertTrue(pipeline.onCooldown(player, superBreaker));
        assertTrue(pipeline.isActive(player, superBreaker));
        assertEquals(Duration.ofSeconds(6), pipeline.activeRemaining(player, superBreaker));
        assertEquals(Duration.ofSeconds(10), pipeline.cooldownRemaining(player, superBreaker));
        assertFalse(pipeline.activate(player, superBreaker));
        assertTrue(pipeline.cancel(player, superBreaker));
        assertFalse(pipeline.isActive(player, superBreaker));
        assertTrue(pipeline.onCooldown(player, superBreaker));
    }
    @Test
    void delegatedRuntimeStateOverridesGenericPipelineState() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);
        NamespacedId superBreaker = NamespacedId.parse("fabricmmo:super_breaker");
        abilities.registerActive(new ActiveAbilityDefinition(superBreaker, CoreSkills.MINING, 1,
                Duration.ofSeconds(4), Duration.ofSeconds(6), Duration.ofSeconds(10), Map.of()));
        abilities.freeze();
        AbilityPipeline pipeline = new AbilityPipeline(abilities, new SimpleEventBus(),
                Clock.fixed(Instant.parse("2026-07-16T12:00:00Z"), ZoneOffset.UTC));
        UUID player = UUID.randomUUID();

        pipeline.registerAbilityStateView(superBreaker, new io.github.njw3995.fabricmmo.api.ability.AbilityStateView() {
            @Override
            public boolean isActive(UUID playerId, NamespacedId abilityId) {
                return player.equals(playerId) && superBreaker.equals(abilityId);
            }

            @Override
            public Duration activeRemaining(UUID playerId, NamespacedId abilityId) {
                return Duration.ofSeconds(3);
            }

            @Override
            public Duration cooldownRemaining(UUID playerId, NamespacedId abilityId) {
                return Duration.ofSeconds(17);
            }
        });

        assertTrue(pipeline.isActive(player, superBreaker));
        assertEquals(Duration.ofSeconds(3), pipeline.activeRemaining(player, superBreaker));
        assertEquals(Duration.ofSeconds(17), pipeline.cooldownRemaining(player, superBreaker));
    }

    @Test
    void stateViewRegistrationValidatesAbilityAndDuplicates() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);
        NamespacedId active = NamespacedId.parse("fabricmmo_test:addon_active");
        abilities.registerActive(new ActiveAbilityDefinition(
                active,
                CoreSkills.MINING,
                1,
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(3),
                Map.of()));
        abilities.freeze();
        AbilityPipeline pipeline = new AbilityPipeline(
                abilities,
                new SimpleEventBus(),
                Clock.fixed(Instant.parse("2026-07-16T12:00:00Z"), ZoneOffset.UTC));
        var view = new io.github.njw3995.fabricmmo.api.ability.AbilityStateView() {
            @Override
            public boolean isActive(UUID playerId, NamespacedId abilityId) {
                return false;
            }

            @Override
            public Duration activeRemaining(UUID playerId, NamespacedId abilityId) {
                return Duration.ZERO;
            }

            @Override
            public Duration cooldownRemaining(UUID playerId, NamespacedId abilityId) {
                return Duration.ZERO;
            }
        };

        pipeline.registerAbilityStateView(active, view);
        pipeline.registerAbilityStateView(active, view);
        assertThrows(IllegalStateException.class,
                () -> pipeline.registerAbilityStateView(active, new io.github.njw3995.fabricmmo.api.ability.AbilityStateView() {
                    @Override
                    public boolean isActive(UUID playerId, NamespacedId abilityId) {
                        return true;
                    }

                    @Override
                    public Duration activeRemaining(UUID playerId, NamespacedId abilityId) {
                        return Duration.ofSeconds(1);
                    }

                    @Override
                    public Duration cooldownRemaining(UUID playerId, NamespacedId abilityId) {
                        return Duration.ofSeconds(1);
                    }
                }));
        assertThrows(IllegalArgumentException.class,
                () -> pipeline.registerAbilityStateView(
                        NamespacedId.parse("fabricmmo_test:missing"), view));
    }

    @Test
    void onlinePreparationValidatesEligibilityPermissionsAndClearsOnDisconnect() {
        NamespacedId active = NamespacedId.parse("fabricmmo_test:validated_active");
        var api = io.github.njw3995.fabricmmo.core.bootstrap.FabricMmoBootstrap.create(
                new io.github.njw3995.fabricmmo.core.persistence.InMemoryProgressionStore(),
                registration -> registration.abilityRegistrar().registerActive(
                        new ActiveAbilityDefinition(
                                active,
                                CoreSkills.MINING,
                                5,
                                Duration.ofSeconds(4),
                                Duration.ofSeconds(6),
                                Duration.ofSeconds(10),
                                Map.of("permission", "fabricmmo_test.ability.validated"))));
        UUID player = UUID.randomUUID();
        api.progression().setLevel(player, CoreSkills.MINING, 5);

        api.abilityPipeline().playerResolver(ignored -> java.util.Optional.of(
                AbilityPipeline.PlayerContext.ineligible()));
        assertFalse(api.abilities().prepareOnline(player, active));

        api.abilityPipeline().playerResolver(ignored -> java.util.Optional.of(
                AbilityPipeline.PlayerContext.eligible(permission -> false)));
        assertFalse(api.abilities().prepareOnline(player, active));

        api.abilityPipeline().playerResolver(ignored -> java.util.Optional.of(
                AbilityPipeline.PlayerContext.eligible(permission ->
                        permission.equals("mcmmo.skills.mining")
                                || permission.equals("fabricmmo_test.ability.validated"))));
        assertTrue(api.abilities().prepareOnline(player, active));
        api.abilityPipeline().playerResolver(ignored -> java.util.Optional.of(
                AbilityPipeline.PlayerContext.eligible(permission -> false)));
        assertFalse(api.abilities().activateOnline(player, active));
        api.abilityPipeline().playerResolver(ignored -> java.util.Optional.of(
                AbilityPipeline.PlayerContext.eligible(permission ->
                        permission.equals("mcmmo.skills.mining")
                                || permission.equals("fabricmmo_test.ability.validated"))));
        assertTrue(api.abilities().activateOnline(player, active));
        assertTrue(api.abilities().isActive(player, active));

        api.abilityPipeline().playerDisconnected(player);
        assertFalse(api.abilities().isActive(player, active));
        assertFalse(api.abilities().onCooldown(player, active));
    }

}

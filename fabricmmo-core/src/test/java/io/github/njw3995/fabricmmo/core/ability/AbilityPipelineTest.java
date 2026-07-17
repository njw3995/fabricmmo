package io.github.njw3995.fabricmmo.core.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        pipeline.registerStateView(superBreaker, new io.github.njw3995.fabricmmo.api.ability.AbilityStateView() {
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

}

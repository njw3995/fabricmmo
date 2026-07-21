package io.github.njw3995.fabricmmo.core.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.api.ability.PassiveResult;
import io.github.njw3995.fabricmmo.api.event.PassiveActivationEvent;
import io.github.njw3995.fabricmmo.core.bootstrap.FabricMmoBootstrap;
import io.github.njw3995.fabricmmo.core.persistence.InMemoryProgressionStore;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PassivePipelineTest {
    private static final NamespacedId PASSIVE = NamespacedId.parse("test:ore_sense");

    @Test
    void checksUnlockPublishesMutableEventAndUsesInjectedRng() {
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), registration ->
                registration.abilityRegistrar().registerPassive(new PassiveDefinition(
                        PASSIVE, CoreSkills.MINING, 10, Map.of())));
        UUID player = UUID.randomUUID();

        PassiveResult locked = api.passives().roll(player, PASSIVE, 0.25D, () -> 0.0D);
        assertEquals(PassiveResult.Status.LOCKED, locked.status());

        api.progression().setLevel(player, CoreSkills.MINING, 10);
        api.events().subscribe(PassiveActivationEvent.class, event -> {
            if (PASSIVE.equals(event.passiveId())) event.resultMultiplier(2.0D);
        });
        PassiveResult activated = api.passives().roll(player, PASSIVE, 0.25D, () -> 0.49D);
        assertTrue(activated.activated());
        assertEquals(0.5D, activated.probability());

        PassiveResult failed = api.passives().roll(player, PASSIVE, 0.25D, () -> 0.75D);
        assertFalse(failed.activated());
    }

    @Test
    void cancellationStopsActivation() {
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), registration ->
                registration.abilityRegistrar().registerPassive(new PassiveDefinition(
                        PASSIVE, CoreSkills.MINING, 0, Map.of())));
        UUID player = UUID.randomUUID();
        api.events().subscribe(PassiveActivationEvent.class, PassiveActivationEvent::cancel);
        PassiveResult result = api.passives().roll(player, PASSIVE, 1.0D, () -> 0.0D);
        assertEquals(PassiveResult.Status.CANCELLED, result.status());
    }
    @Test
    void onlineRollValidatesEligibilityAndPermissions() {
        NamespacedId passive = NamespacedId.parse("fabricmmo_test:validated_passive");
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), registration ->
                registration.abilityRegistrar().registerPassive(new PassiveDefinition(
                        passive,
                        CoreSkills.MINING,
                        5,
                        Map.of("permission", "fabricmmo_test.passive.validated"))));
        UUID player = UUID.randomUUID();
        api.progression().setLevel(player, CoreSkills.MINING, 5);

        api.passivePipeline().playerResolver(ignored -> java.util.Optional.of(
                PassivePipeline.PlayerContext.ineligible()));
        assertEquals(PassiveResult.Status.INELIGIBLE,
                api.passives().rollOnline(player, passive, 1.0D, () -> 0.0D).status());

        api.passivePipeline().playerResolver(ignored -> java.util.Optional.of(
                PassivePipeline.PlayerContext.eligible(permission -> false)));
        assertEquals(PassiveResult.Status.INELIGIBLE,
                api.passives().rollOnline(player, passive, 1.0D, () -> 0.0D).status());

        api.passivePipeline().playerResolver(ignored -> java.util.Optional.of(
                PassivePipeline.PlayerContext.eligible(permission ->
                        permission.equals("mcmmo.skills.mining")
                                || permission.equals("fabricmmo_test.passive.validated"))));
        assertEquals(PassiveResult.Status.ACTIVATED,
                api.passives().rollOnline(player, passive, 1.0D, () -> 0.0D).status());
    }

}

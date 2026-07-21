package io.github.njw3995.fabricmmo.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.bootstrap.FabricMmoBootstrap;
import io.github.njw3995.fabricmmo.core.persistence.InMemoryProgressionStore;
import io.github.njw3995.fabricmmo.testaddon.TestAddonRegistration;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TestAddonApiValidationTest {
    @Test
    void externalStyleAddonUsesEveryMilestoneOneRegistrationSurface() {
        TestAddonRegistration addon = new TestAddonRegistration();
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), addon::register);

        assertTrue(api.skillRegistry().find(TestAddonRegistration.ENGINEERING).isPresent());
        assertEquals(1, api.skillRegistry().extensions(NamespacedId.parse("fabricmmo:mining")).size());
        assertTrue(api.xpSources().find(TestAddonRegistration.TEST_SOURCE).isPresent());
        assertEquals(125, api.gatheringContent().find(TestAddonRegistration.TEST_ORE)
                .orElseThrow().xp());
        assertEquals(3.25D, api.entityXpContent().find(TestAddonRegistration.TEST_ENTITY_XP)
                .orElseThrow().xp());
        assertEquals(2, api.brewingContent().find(TestAddonRegistration.TEST_BREW)
                .orElseThrow().stage());
        assertEquals(1, api.configRegistry().contributions().size());
        assertEquals("engineering", api.commandMetadata()
                .find(NamespacedId.parse("fabricmmo_test_addon:engineering_command"))
                .orElseThrow()
                .literal());
        assertEquals("minecraft:redstone",
                api.uiMetadata().findForSkill(TestAddonRegistration.ENGINEERING).orElseThrow().iconId());
        assertTrue(api.skillRegistry().frozen());
        assertTrue(api.xpSources().frozen());
        assertTrue(api.commandMetadata().frozen());
        assertTrue(api.configRegistry().frozen());
        assertTrue(api.gatheringContent().frozen());
        assertTrue(api.entityXpContent().frozen());
        assertTrue(api.brewingContent().frozen());
        assertTrue(api.uiMetadata().frozen());
        UUID statePlayer = UUID.randomUUID();
        assertTrue(api.abilityStates().isActive(statePlayer, TestAddonRegistration.OVERCLOCK));
        assertEquals(Duration.ofSeconds(8), api.abilityStates().activeRemaining(
                statePlayer, TestAddonRegistration.OVERCLOCK));
        assertTrue(api.protection().canBreak(
                statePlayer, "minecraft:overworld", 0, 64, 0));
        assertTrue(api.protection().frozen());
        assertFalse(api.persistentMarkers().available());

        api.events().publish(new TestAddonRegistration.MachineCompleted(
                UUID.randomUUID(),
                NamespacedId.parse("fabricmmo_test_addon:test_machine"),
                1020));
        assertEquals(1, addon.observedLevelEvents());
    }
}

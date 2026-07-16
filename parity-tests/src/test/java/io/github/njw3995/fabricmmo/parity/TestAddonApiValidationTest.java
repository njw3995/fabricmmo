package io.github.njw3995.fabricmmo.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.core.bootstrap.FabricMmoBootstrap;
import io.github.njw3995.fabricmmo.core.persistence.InMemoryProgressionStore;
import io.github.njw3995.fabricmmo.testaddon.TestAddonRegistration;
import java.util.Map;
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
        assertEquals(1, api.configRegistry().contributions().size());
        assertEquals("engineering", api.commandMetadata().commands().getFirst().literal());
        assertEquals("minecraft:redstone",
                api.uiMetadata().findForSkill(TestAddonRegistration.ENGINEERING).orElseThrow().iconId());
        assertTrue(api.skillRegistry().frozen());
        assertTrue(api.xpSources().frozen());
        assertTrue(api.commandMetadata().frozen());
        assertTrue(api.configRegistry().frozen());
        assertTrue(api.uiMetadata().frozen());

        api.progression().award(new XpAwardRequest(
                UUID.randomUUID(),
                TestAddonRegistration.ENGINEERING,
                TestAddonRegistration.TEST_SOURCE,
                1020,
                Map.of()));
        assertEquals(1, addon.observedLevelEvents());
    }
}

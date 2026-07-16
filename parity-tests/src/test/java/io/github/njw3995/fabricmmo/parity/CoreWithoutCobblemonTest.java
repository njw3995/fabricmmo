package io.github.njw3995.fabricmmo.parity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.bootstrap.FabricMmoBootstrap;
import io.github.njw3995.fabricmmo.core.persistence.InMemoryProgressionStore;
import org.junit.jupiter.api.Test;

class CoreWithoutCobblemonTest {
    @Test
    void coreBootstrapsWithoutCobblemonClassesOrSkills() {
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), ignored -> { });
        assertTrue(api.skillRegistry().find(NamespacedId.parse("fabricmmo:mining")).isPresent());
        assertFalse(api.skillRegistry().find(
                NamespacedId.parse("fabricmmo_cobblemon:training")).isPresent());
    }
}

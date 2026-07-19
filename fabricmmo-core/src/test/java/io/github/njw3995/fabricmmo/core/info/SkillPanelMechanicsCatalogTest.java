package io.github.njw3995.fabricmmo.core.info;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SkillPanelMechanicsCatalogTest {
    @Test
    void unimplementedSkillsDoNotAdvertiseFabricatedStats() {
        SkillPanelMechanicsCatalog catalog = new SkillPanelMechanicsCatalog();

        assertTrue(catalog.provider(CoreSkills.ACROBATICS).rows(UUID.randomUUID(), 1000).isEmpty());
        assertTrue(catalog.provider(CoreSkills.FISHING).rows(UUID.randomUUID(), 1000).isEmpty());
        assertTrue(catalog.provider(CoreSkills.SMELTING).rows(UUID.randomUUID(), 1000).isEmpty());
    }
}

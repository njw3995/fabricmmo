package io.github.njw3995.fabricmmo.core.permission;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExcavationPermissionNodesTest {
    @Test
    void usesUpstreamExcavationPermissionNodes() {
        assertEquals("mcmmo.skills.excavation", PermissionNodes.EXCAVATION);
        assertEquals("mcmmo.commands.excavation", PermissionNodes.EXCAVATION_COMMAND);
        assertEquals("mcmmo.ability.excavation.gigadrillbreaker",
                PermissionNodes.EXCAVATION_GIGA_DRILL_BREAKER);
        assertEquals("mcmmo.ability.excavation.archaeology",
                PermissionNodes.EXCAVATION_ARCHAEOLOGY);
        assertEquals("mcmmo.perks.lucky.excavation", PermissionNodes.EXCAVATION_LUCKY);
    }
}

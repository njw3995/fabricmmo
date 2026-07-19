package io.github.njw3995.fabricmmo.core.skill.excavation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CoreExcavationAbilitiesTest {
    @Test
    void registersCompleteExcavationAbilityFamily() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);

        CoreExcavationAbilities.registerAll(abilities);

        assertEquals(Set.of(CoreExcavationAbilities.GIGA_DRILL_BREAKER),
                abilities.actives().stream()
                        .map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertEquals(Set.of(CoreExcavationAbilities.ARCHAEOLOGY),
                abilities.passives().stream()
                        .map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertTrue(abilities.actives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.EXCAVATION)));
        assertTrue(abilities.passives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.EXCAVATION)));
    }
}

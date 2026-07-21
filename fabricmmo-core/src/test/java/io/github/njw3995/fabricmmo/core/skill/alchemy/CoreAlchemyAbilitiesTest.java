package io.github.njw3995.fabricmmo.core.skill.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CoreAlchemyAbilitiesTest {
    @Test
    void registersBothPinnedAlchemySubskills() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);
        CoreAlchemyAbilities.registerAll(abilities);
        assertTrue(abilities.actives().isEmpty());
        assertEquals(Set.of(CoreAlchemyAbilities.CATALYSIS, CoreAlchemyAbilities.CONCOCTIONS),
                abilities.passives().stream().map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertTrue(abilities.passives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.ALCHEMY)));
    }
}

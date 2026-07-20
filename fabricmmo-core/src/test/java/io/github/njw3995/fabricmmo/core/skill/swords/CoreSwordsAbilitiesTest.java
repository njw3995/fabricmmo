package io.github.njw3995.fabricmmo.core.skill.swords;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CoreSwordsAbilitiesTest {
    @Test
    void registersTheCompleteSwordsAbilityFamily() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);

        CoreSwordsAbilities.registerAll(abilities);

        assertEquals(Set.of(CoreSwordsAbilities.SERRATED_STRIKES),
                abilities.actives().stream().map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertEquals(Set.of(
                        CoreSwordsAbilities.COUNTER_ATTACK,
                        CoreSwordsAbilities.RUPTURE,
                        CoreSwordsAbilities.STAB,
                        CoreSwordsAbilities.LIMIT_BREAK),
                abilities.passives().stream().map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertTrue(abilities.actives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.SWORDS)));
        assertTrue(abilities.passives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.SWORDS)));
    }
}

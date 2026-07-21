package io.github.njw3995.fabricmmo.core.skill.axes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CoreAxesAbilitiesTest {
    @Test
    void registersTheCompleteAxesAbilityFamily() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);

        CoreAxesAbilities.registerAll(abilities);

        assertEquals(Set.of(CoreAxesAbilities.SKULL_SPLITTER),
                abilities.actives().stream().map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertEquals(Set.of(
                        CoreAxesAbilities.CRITICAL_STRIKES,
                        CoreAxesAbilities.AXE_MASTERY,
                        CoreAxesAbilities.ARMOR_IMPACT,
                        CoreAxesAbilities.GREATER_IMPACT,
                        CoreAxesAbilities.LIMIT_BREAK),
                abilities.passives().stream().map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertTrue(abilities.actives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.AXES)));
        assertTrue(abilities.passives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.AXES)));
    }
}

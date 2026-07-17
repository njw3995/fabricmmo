package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CoreMiningAbilitiesTest {
    @Test
    void registersCompleteMiningAbilityFamily() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);

        CoreMiningAbilities.registerAll(abilities);

        assertEquals(Set.of(CoreMiningAbilities.SUPER_BREAKER, CoreMiningAbilities.BLAST_MINING),
                abilities.actives().stream().map(definition -> definition.id()).collect(Collectors.toSet()));
        assertEquals(Set.of(
                        CoreMiningAbilities.DOUBLE_DROPS,
                        CoreMiningAbilities.MOTHER_LODE,
                        CoreMiningAbilities.BIGGER_BOMBS,
                        CoreMiningAbilities.DEMOLITIONS_EXPERTISE),
                abilities.passives().stream().map(definition -> definition.id()).collect(Collectors.toSet()));
        assertTrue(abilities.actives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.MINING)));
        assertTrue(abilities.passives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.MINING)));
    }
}

package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CoreWoodcuttingAbilitiesTest {
    @Test
    void registersCompleteWoodcuttingAbilityFamily() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);

        CoreWoodcuttingAbilities.registerAll(abilities);

        assertEquals(Set.of(CoreWoodcuttingAbilities.TREE_FELLER),
                abilities.actives().stream()
                        .map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertEquals(Set.of(
                        CoreWoodcuttingAbilities.HARVEST_LUMBER,
                        CoreWoodcuttingAbilities.CLEAN_CUTS,
                        CoreWoodcuttingAbilities.KNOCK_ON_WOOD,
                        CoreWoodcuttingAbilities.LEAF_BLOWER),
                abilities.passives().stream()
                        .map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertTrue(abilities.actives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.WOODCUTTING)));
        assertTrue(abilities.passives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.WOODCUTTING)));
    }
}

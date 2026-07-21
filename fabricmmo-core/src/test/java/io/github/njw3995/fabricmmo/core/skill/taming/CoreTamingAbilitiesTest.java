package io.github.njw3995.fabricmmo.core.skill.taming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CoreTamingAbilitiesTest {
    @Test
    void registersCompletePinnedTamingAbilityFamily() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);
        CoreTamingAbilities.registerAll(abilities);
        assertTrue(abilities.actives().isEmpty());
        assertEquals(Set.of(CoreTamingAbilities.BEAST_LORE,
                        CoreTamingAbilities.CALL_OF_THE_WILD, CoreTamingAbilities.GORE,
                        CoreTamingAbilities.SHARPENED_CLAWS,
                        CoreTamingAbilities.ENVIRONMENTALLY_AWARE,
                        CoreTamingAbilities.THICK_FUR, CoreTamingAbilities.HOLY_HOUND,
                        CoreTamingAbilities.SHOCK_PROOF, CoreTamingAbilities.FAST_FOOD_SERVICE,
                        CoreTamingAbilities.PUMMEL),
                abilities.passives().stream().map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertTrue(abilities.passives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.TAMING)));
    }
}

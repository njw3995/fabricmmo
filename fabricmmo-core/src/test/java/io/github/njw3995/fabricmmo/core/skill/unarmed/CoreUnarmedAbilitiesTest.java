package io.github.njw3995.fabricmmo.core.skill.unarmed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CoreUnarmedAbilitiesTest {
    @Test
    void registersTheCompleteUnarmedAbilityFamily() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);

        CoreUnarmedAbilities.registerAll(abilities);

        assertEquals(Set.of(CoreUnarmedAbilities.BERSERK),
                abilities.actives().stream().map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertEquals(Set.of(
                        CoreUnarmedAbilities.ARROW_DEFLECT,
                        CoreUnarmedAbilities.DISARM,
                        CoreUnarmedAbilities.IRON_GRIP,
                        CoreUnarmedAbilities.STEEL_ARM_STYLE,
                        CoreUnarmedAbilities.BLOCK_CRACKER,
                        CoreUnarmedAbilities.LIMIT_BREAK),
                abilities.passives().stream().map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertTrue(abilities.actives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.UNARMED)));
        assertTrue(abilities.passives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.UNARMED)));
        assertEquals(0, abilities.passives().stream()
                .filter(definition -> definition.id().equals(CoreUnarmedAbilities.BLOCK_CRACKER))
                .findFirst().orElseThrow().unlockLevel());
    }
}

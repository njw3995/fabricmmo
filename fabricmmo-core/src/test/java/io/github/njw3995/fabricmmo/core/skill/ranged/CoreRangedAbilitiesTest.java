package io.github.njw3995.fabricmmo.core.skill.ranged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import io.github.njw3995.fabricmmo.core.skill.archery.CoreArcheryAbilities;
import io.github.njw3995.fabricmmo.core.skill.crossbows.CoreCrossbowsAbilities;
import io.github.njw3995.fabricmmo.core.skill.tridents.CoreTridentsAbilities;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CoreRangedAbilitiesTest {
    @Test
    void registersAllPinnedRangedPassivesWithoutInventingActives() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);

        CoreArcheryAbilities.registerAll(abilities);
        CoreCrossbowsAbilities.registerAll(abilities);
        CoreTridentsAbilities.registerAll(abilities);

        assertTrue(abilities.actives().isEmpty());
        assertEquals(Set.of(
                        CoreArcheryAbilities.ARROW_RETRIEVAL,
                        CoreArcheryAbilities.DAZE,
                        CoreArcheryAbilities.SKILL_SHOT,
                        CoreArcheryAbilities.LIMIT_BREAK,
                        CoreCrossbowsAbilities.POWERED_SHOT,
                        CoreCrossbowsAbilities.TRICK_SHOT,
                        CoreCrossbowsAbilities.LIMIT_BREAK,
                        CoreTridentsAbilities.IMPALE,
                        CoreTridentsAbilities.LIMIT_BREAK),
                abilities.passives().stream().map(definition -> definition.id())
                        .collect(Collectors.toSet()));
    }
}

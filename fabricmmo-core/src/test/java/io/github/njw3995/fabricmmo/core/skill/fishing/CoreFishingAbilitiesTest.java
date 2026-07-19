package io.github.njw3995.fabricmmo.core.skill.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.ability.DefaultAbilityRegistry;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CoreFishingAbilitiesTest {
    @Test
    void registersCompleteFishingPassiveFamily() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultAbilityRegistry abilities = new DefaultAbilityRegistry(skills);

        CoreFishingAbilities.registerAll(abilities);

        assertTrue(abilities.actives().isEmpty());
        assertEquals(Set.of(
                        CoreFishingAbilities.TREASURE_HUNTER,
                        CoreFishingAbilities.MAGIC_HUNTER,
                        CoreFishingAbilities.SHAKE,
                        CoreFishingAbilities.FISHERMANS_DIET,
                        CoreFishingAbilities.MASTER_ANGLER,
                        CoreFishingAbilities.ICE_FISHING,
                        CoreFishingAbilities.VANILLA_XP_BOOST),
                abilities.passives().stream()
                        .map(definition -> definition.id())
                        .collect(Collectors.toSet()));
        assertTrue(abilities.passives().stream()
                .allMatch(definition -> definition.skillId().equals(CoreSkills.FISHING)));
    }
}

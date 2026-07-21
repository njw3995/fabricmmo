package io.github.njw3995.fabricmmo.core.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class CoreSkillsTest {
    @Test
    void implementedSkillSetTracksGameplayCompleteCoreSkills() {
        assertEquals(Set.of(
                CoreSkills.ACROBATICS,
                CoreSkills.AXES,
                CoreSkills.EXCAVATION,
                CoreSkills.FISHING,
                CoreSkills.HERBALISM,
                CoreSkills.MACES,
                CoreSkills.MINING,
                CoreSkills.SWORDS,
                CoreSkills.UNARMED,
                CoreSkills.WOODCUTTING),
                CoreSkills.implementedSkillIds());
    }
}

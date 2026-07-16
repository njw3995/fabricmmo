package io.github.njw3995.fabricmmo.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.skill.SkillCategory;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultSkillRegistryTest {
    @Test
    void detectsDuplicatesAndRejectsLateRegistration() {
        DefaultSkillRegistry registry = new DefaultSkillRegistry();
        CoreSkills.registerAll(registry);
        assertThrows(IllegalStateException.class, () -> CoreSkills.registerAll(registry));
        registry.freeze();
        assertThrows(IllegalStateException.class, () -> registry.registerSkill(new SkillDefinition(
                new NamespacedId("test", "late"), SkillCategory.ADDON, "test.late", 100,
                true, List.of(), Map.of())));
    }

    @Test
    void orderingIsDeterministic() {
        DefaultSkillRegistry registry = new DefaultSkillRegistry();
        CoreSkills.registerAll(registry);
        registry.freeze();
        assertEquals(CoreSkills.ACROBATICS, registry.skills().get(0).id());
        assertEquals(18, registry.skills().size());
    }
}

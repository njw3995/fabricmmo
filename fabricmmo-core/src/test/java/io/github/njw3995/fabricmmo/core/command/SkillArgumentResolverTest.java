package io.github.njw3995.fabricmmo.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.skill.SkillCategory;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SkillArgumentResolverTest {
    @Test
    void resolvesCorePathsAndNamespacedAddonSkills() {
        DefaultSkillRegistry registry = registry();

        assertEquals(NamespacedId.parse("fabricmmo:mining"),
                SkillArgumentResolver.resolve(registry, "Mining", false).orElseThrow().id());
        assertEquals(NamespacedId.parse("example:mining"),
                SkillArgumentResolver.resolve(registry, "example:mining", false).orElseThrow().id());
        assertTrue(SkillArgumentResolver.resolve(registry, "unknown", false).isEmpty());
        assertTrue(SkillArgumentResolver.resolve(registry, "bad:id:syntax", false).isEmpty());
    }

    @Test
    void suggestionsUseShortCoreNamesAndNamespacedAddonNames() {
        assertEquals(List.of("all", "example:mining", "mining"),
                SkillArgumentResolver.suggestions(registry(), true));
    }

    private static DefaultSkillRegistry registry() {
        DefaultSkillRegistry registry = new DefaultSkillRegistry();
        registry.registerSkill(skill("fabricmmo:mining"));
        registry.registerSkill(skill("example:mining"));
        return registry;
    }

    private static SkillDefinition skill(String id) {
        NamespacedId skillId = NamespacedId.parse(id);
        return new SkillDefinition(skillId, SkillCategory.GATHERING,
                "skill." + skillId.namespace() + '.' + skillId.path(), 1000,
                true, List.of(), Map.of());
    }
}

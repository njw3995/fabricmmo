package io.github.njw3995.fabricmmo.core.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.content.ContentSelector;
import io.github.njw3995.fabricmmo.api.content.GatheringContentDefinition;
import io.github.njw3995.fabricmmo.api.content.MaturityRequirement;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import io.github.njw3995.fabricmmo.core.progression.ProgressionSettings;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DefaultGatheringContentRegistryTest {
    @Test
    void validatesSkillsOrdersDefinitionsAndFreezes() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills, ProgressionSettings.upstreamDefaults());
        DefaultGatheringContentRegistry registry = new DefaultGatheringContentRegistry(skills);
        GatheringContentDefinition definition = definition("test:ore", "fabricmmo:mining");

        registry.registerGatheringContent(definition);
        registry.freeze();

        assertEquals(definition, registry.find(NamespacedId.parse("test:ore")).orElseThrow());
        assertEquals(1, registry.definitionsForSkill(NamespacedId.parse("fabricmmo:mining")).size());
        assertTrue(registry.frozen());
        assertThrows(IllegalStateException.class,
                () -> registry.registerGatheringContent(definition("test:late", "fabricmmo:mining")));
    }

    @Test
    void rejectsUnknownSkillsAndDuplicateIds() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills, ProgressionSettings.upstreamDefaults());
        DefaultGatheringContentRegistry registry = new DefaultGatheringContentRegistry(skills);
        GatheringContentDefinition definition = definition("test:ore", "fabricmmo:mining");
        registry.registerGatheringContent(definition);

        assertThrows(IllegalStateException.class,
                () -> registry.registerGatheringContent(definition));
        assertThrows(IllegalStateException.class,
                () -> registry.registerGatheringContent(definition("test:unknown", "test:missing")));
    }


    @Test
    void datapackLayerOverridesAndDisablesFrozenJavaDefinitions() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills, ProgressionSettings.upstreamDefaults());
        DefaultGatheringContentRegistry registry = new DefaultGatheringContentRegistry(skills);
        GatheringContentDefinition code = definition("test:ore", "fabricmmo:mining");
        GatheringContentDefinition override = new GatheringContentDefinition(
                code.id(), code.skillId(), code.block(), 999, code.validTools(),
                code.naturalBlocksOnly(), code.maturity(), code.bonusDrops(),
                code.activeAbility(), code.replant(), code.metadata());
        registry.registerGatheringContent(code);
        registry.freeze();

        registry.replaceDatapackDefinitions(java.util.List.of(override), java.util.Set.of());
        assertEquals(999, registry.find(code.id()).orElseThrow().xp());
        assertEquals(1, registry.codeDefinitionCount());
        assertEquals(1, registry.datapackDefinitionCount());

        registry.replaceDatapackDefinitions(java.util.List.of(), java.util.Set.of(code.id()));
        assertTrue(registry.find(code.id()).isEmpty());
        assertEquals(1, registry.datapackDisabledCount());
    }

    private static GatheringContentDefinition definition(String id, String skill) {
        return new GatheringContentDefinition(
                NamespacedId.parse(id),
                NamespacedId.parse(skill),
                ContentSelector.tag("test:ores"),
                100,
                Set.of(ContentSelector.tag("minecraft:pickaxes")),
                true,
                MaturityRequirement.any(),
                true,
                true,
                Optional.empty(),
                Map.of());
    }
}

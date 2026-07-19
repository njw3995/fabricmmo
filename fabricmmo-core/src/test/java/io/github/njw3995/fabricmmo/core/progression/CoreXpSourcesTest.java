package io.github.njw3995.fabricmmo.core.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.skill.SkillCategory;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CoreXpSourcesTest {
    @Test
    void registersCommandSourcesForCoreAndAddonPrimarySkills() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        NamespacedId addonSkill = NamespacedId.parse("example:training");
        skills.registerSkill(new SkillDefinition(addonSkill, SkillCategory.ADDON,
                "skill.example.training", 1000, true, List.of(), Map.of()));
        DefaultXpSourceRegistry sources = new DefaultXpSourceRegistry(skills);

        CoreXpSources.registerCommandSources(skills.skills(), sources);

        assertTrue(sources.find(CoreXpSources.commandSourceId(CoreSkills.MINING)).isPresent());
        assertTrue(sources.find(CoreXpSources.commandSourceId(addonSkill)).isPresent());
        assertEquals(17, sources.sources().size());
    }
    @Test
    void registersSeparateMiningBlockAndBlastSources() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultXpSourceRegistry sources = new DefaultXpSourceRegistry(skills);

        CoreXpSources.registerDefaults(sources);

        assertTrue(sources.find(CoreXpSources.MINING_BLOCK_BREAK).isPresent());
        assertTrue(sources.find(CoreXpSources.MINING_BLAST).isPresent());
        long miningSources = sources.sources().stream()
                .filter(source -> source.skillId().equals(CoreSkills.MINING))
                .count();
        assertEquals(2, miningSources);
    }

    @Test
    void registersGatheringXpSources() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultXpSourceRegistry sources = new DefaultXpSourceRegistry(skills);

        CoreXpSources.registerDefaults(sources);

        assertTrue(sources.find(CoreXpSources.EXCAVATION_BLOCK_BREAK).isPresent());
        assertTrue(sources.find(CoreXpSources.EXCAVATION_TREASURE).isPresent());
        assertTrue(sources.find(CoreXpSources.HERBALISM_BLOCK_BREAK).isPresent());
        assertTrue(sources.find(CoreXpSources.HERBALISM_BERRY_HARVEST).isPresent());
        assertTrue(sources.find(CoreXpSources.HERBALISM_HYLIAN_LUCK).isPresent());
        long excavationSources = sources.sources().stream()
                .filter(source -> source.skillId().equals(CoreSkills.EXCAVATION))
                .count();
        assertEquals(2, excavationSources);
        long herbalismSources = sources.sources().stream()
                .filter(source -> source.skillId().equals(CoreSkills.HERBALISM))
                .count();
        assertEquals(3, herbalismSources);
    }

}

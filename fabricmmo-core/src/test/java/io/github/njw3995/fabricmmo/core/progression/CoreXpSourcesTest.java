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

    @Test
    void registersAcrobaticsFallAndDodgeSources() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultXpSourceRegistry sources = new DefaultXpSourceRegistry(skills);

        CoreXpSources.registerDefaults(sources);

        assertTrue(sources.find(CoreXpSources.ACROBATICS_FALL).isPresent());
        assertTrue(sources.find(CoreXpSources.ACROBATICS_DODGE).isPresent());
        long acrobaticsSources = sources.sources().stream()
                .filter(source -> source.skillId().equals(CoreSkills.ACROBATICS))
                .count();
        assertEquals(2, acrobaticsSources);
    }

    @Test
    void registersAxesCombatSource() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultXpSourceRegistry sources = new DefaultXpSourceRegistry(skills);

        CoreXpSources.registerDefaults(sources);

        assertTrue(sources.find(CoreXpSources.AXES_COMBAT).isPresent());
        long axesSources = sources.sources().stream()
                .filter(source -> source.skillId().equals(CoreSkills.AXES))
                .count();
        assertEquals(1, axesSources);
    }

    @Test
    void registersMacesCombatSource() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultXpSourceRegistry sources = new DefaultXpSourceRegistry(skills);

        CoreXpSources.registerDefaults(sources);

        assertTrue(sources.find(CoreXpSources.MACES_COMBAT).isPresent());
        long macesSources = sources.sources().stream()
                .filter(source -> source.skillId().equals(CoreSkills.MACES))
                .count();
        assertEquals(1, macesSources);
    }

    @Test
    void registersSwordsCombatSource() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultXpSourceRegistry sources = new DefaultXpSourceRegistry(skills);

        CoreXpSources.registerDefaults(sources);

        assertTrue(sources.find(CoreXpSources.SWORDS_COMBAT).isPresent());
        long swordsSources = sources.sources().stream()
                .filter(source -> source.skillId().equals(CoreSkills.SWORDS))
                .count();
        assertEquals(1, swordsSources);
    }

    @Test
    void registersUnarmedCombatSource() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultXpSourceRegistry sources = new DefaultXpSourceRegistry(skills);

        CoreXpSources.registerDefaults(sources);

        assertTrue(sources.find(CoreXpSources.UNARMED_COMBAT).isPresent());
        long unarmedSources = sources.sources().stream()
                .filter(source -> source.skillId().equals(CoreSkills.UNARMED))
                .count();
        assertEquals(1, unarmedSources);
    }

    @Test
    void registersTamingAndAlchemyXpSources() {
        DefaultSkillRegistry skills = new DefaultSkillRegistry();
        CoreSkills.registerAll(skills);
        DefaultXpSourceRegistry sources = new DefaultXpSourceRegistry(skills);

        CoreXpSources.registerDefaults(sources);

        assertTrue(sources.find(CoreXpSources.TAMING_ANIMAL_TAMED).isPresent());
        assertTrue(sources.find(CoreXpSources.TAMING_PET_COMBAT).isPresent());
        assertTrue(sources.find(CoreXpSources.ALCHEMY_BREW).isPresent());
        assertEquals(2, sources.sources().stream()
                .filter(source -> source.skillId().equals(CoreSkills.TAMING)).count());
        assertEquals(1, sources.sources().stream()
                .filter(source -> source.skillId().equals(CoreSkills.ALCHEMY)).count());
    }

}

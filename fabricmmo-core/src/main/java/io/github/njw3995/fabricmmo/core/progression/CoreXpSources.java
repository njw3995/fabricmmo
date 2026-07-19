package io.github.njw3995.fabricmmo.core.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.XpSourceDefinition;
import io.github.njw3995.fabricmmo.api.progression.XpSourceRegistrar;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;

public final class CoreXpSources {
    public static final NamespacedId MINING_BLOCK_BREAK = NamespacedId.parse("fabricmmo:mining_block_break");
    public static final NamespacedId MINING_BLAST = NamespacedId.parse("fabricmmo:mining_blast");
    public static final NamespacedId WOODCUTTING_BLOCK_BREAK =
            NamespacedId.parse("fabricmmo:woodcutting_block_break");
    public static final NamespacedId WOODCUTTING_TREE_FELLER =
            NamespacedId.parse("fabricmmo:woodcutting_tree_feller");
    public static final NamespacedId EXCAVATION_BLOCK_BREAK =
            NamespacedId.parse("fabricmmo:excavation_block_break");
    public static final NamespacedId EXCAVATION_TREASURE =
            NamespacedId.parse("fabricmmo:excavation_treasure");

    private CoreXpSources() {
    }

    public static void registerDefaults(XpSourceRegistrar registrar) {
        registrar.registerXpSource(new XpSourceDefinition(
                MINING_BLOCK_BREAK,
                CoreSkills.MINING,
                Map.of("upstream", "MiningManager#miningBlockCheck")));
        registrar.registerXpSource(new XpSourceDefinition(
                MINING_BLAST,
                CoreSkills.MINING,
                Map.of("upstream", "MiningManager#blastMiningDropProcessing")));
        registrar.registerXpSource(new XpSourceDefinition(
                WOODCUTTING_BLOCK_BREAK,
                CoreSkills.WOODCUTTING,
                Map.of("upstream", "WoodcuttingManager#processWoodcuttingBlock")));
        registrar.registerXpSource(new XpSourceDefinition(
                WOODCUTTING_TREE_FELLER,
                CoreSkills.WOODCUTTING,
                Map.of("upstream", "WoodcuttingManager#processTreeFeller")));
        registrar.registerXpSource(new XpSourceDefinition(
                EXCAVATION_BLOCK_BREAK,
                CoreSkills.EXCAVATION,
                Map.of("upstream", "ExcavationManager#excavationBlockCheck")));
        registrar.registerXpSource(new XpSourceDefinition(
                EXCAVATION_TREASURE,
                CoreSkills.EXCAVATION,
                Map.of("upstream", "ExcavationManager#rollAndCollectTreasureDrops")));
    }

    public static void registerCommandSources(
            Iterable<SkillDefinition> skills,
            XpSourceRegistrar registrar) {
        for (SkillDefinition skill : skills) {
            if (skill.childSkill()) {
                continue;
            }
            registrar.registerXpSource(new XpSourceDefinition(
                    commandSourceId(skill.id()),
                    skill.id(),
                    Map.of(
                            "upstreamReason", "XPGainReason.COMMAND",
                            "upstreamSource", "XPGainSource.COMMAND")));
        }
    }

    public static NamespacedId commandSourceId(NamespacedId skillId) {
        return new NamespacedId("fabricmmo", "command/" + skillId.namespace() + '/' + skillId.path());
    }
}

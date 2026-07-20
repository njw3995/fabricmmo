package io.github.njw3995.fabricmmo.core.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.XpSourceDefinition;
import io.github.njw3995.fabricmmo.api.progression.XpSourceRegistrar;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;

public final class CoreXpSources {
    public static final NamespacedId ACROBATICS_FALL =
            NamespacedId.parse("fabricmmo:acrobatics_fall");
    public static final NamespacedId ACROBATICS_DODGE =
            NamespacedId.parse("fabricmmo:acrobatics_dodge");
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
    public static final NamespacedId HERBALISM_BLOCK_BREAK =
            NamespacedId.parse("fabricmmo:herbalism_block_break");
    public static final NamespacedId HERBALISM_BERRY_HARVEST =
            NamespacedId.parse("fabricmmo:herbalism_berry_harvest");
    public static final NamespacedId HERBALISM_HYLIAN_LUCK =
            NamespacedId.parse("fabricmmo:herbalism_hylian_luck");
    public static final NamespacedId FISHING_CATCH =
            NamespacedId.parse("fabricmmo:fishing_catch");
    public static final NamespacedId FISHING_SHAKE =
            NamespacedId.parse("fabricmmo:fishing_shake");
    public static final NamespacedId ARCHERY_COMBAT =
            NamespacedId.parse("fabricmmo:archery_combat");
    public static final NamespacedId CROSSBOWS_COMBAT =
            NamespacedId.parse("fabricmmo:crossbows_combat");
    public static final NamespacedId TRIDENTS_COMBAT =
            NamespacedId.parse("fabricmmo:tridents_combat");
    public static final NamespacedId SWORDS_COMBAT =
            NamespacedId.parse("fabricmmo:swords_combat");

    private CoreXpSources() {
    }

    public static void registerDefaults(XpSourceRegistrar registrar) {
        registrar.registerXpSource(new XpSourceDefinition(
                ACROBATICS_FALL,
                CoreSkills.ACROBATICS,
                Map.of("upstream", "Roll#rollCheck")));
        registrar.registerXpSource(new XpSourceDefinition(
                ACROBATICS_DODGE,
                CoreSkills.ACROBATICS,
                Map.of("upstream", "AcrobaticsManager#dodgeCheck")));
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
        registrar.registerXpSource(new XpSourceDefinition(
                HERBALISM_BLOCK_BREAK,
                CoreSkills.HERBALISM,
                Map.of("upstream", "HerbalismManager#processHerbalismBlockBreakEvent")));
        registrar.registerXpSource(new XpSourceDefinition(
                HERBALISM_BERRY_HARVEST,
                CoreSkills.HERBALISM,
                Map.of("upstream", "HerbalismManager#processBerryBushHarvesting")));
        registrar.registerXpSource(new XpSourceDefinition(
                HERBALISM_HYLIAN_LUCK,
                CoreSkills.HERBALISM,
                Map.of("upstream", "HerbalismManager#processHylianLuck")));
        registrar.registerXpSource(new XpSourceDefinition(
                FISHING_CATCH,
                CoreSkills.FISHING,
                Map.of("upstream", "FishingManager#processFishing")));
        registrar.registerXpSource(new XpSourceDefinition(
                FISHING_SHAKE,
                CoreSkills.FISHING,
                Map.of("upstream", "FishingManager#shakeCheck")));
        registrar.registerXpSource(new XpSourceDefinition(
                ARCHERY_COMBAT,
                CoreSkills.ARCHERY,
                Map.of("upstream", "CombatUtils#processArcheryCombat")));
        registrar.registerXpSource(new XpSourceDefinition(
                CROSSBOWS_COMBAT,
                CoreSkills.CROSSBOWS,
                Map.of("upstream", "CombatUtils#processCrossbowsCombat")));
        registrar.registerXpSource(new XpSourceDefinition(
                TRIDENTS_COMBAT,
                CoreSkills.TRIDENTS,
                Map.of("upstream", "CombatUtils#processTridentsCombat")));
        registrar.registerXpSource(new XpSourceDefinition(
                SWORDS_COMBAT,
                CoreSkills.SWORDS,
                Map.of("upstream", "CombatUtils#processSwordCombat")));
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

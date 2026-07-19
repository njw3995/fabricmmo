package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.time.Duration;
import java.util.Map;

/** Public API definitions for the mcMMO 2.3.000 Woodcutting ability family. */
public final class CoreWoodcuttingAbilities {
    public static final NamespacedId TREE_FELLER = id("tree_feller");
    public static final NamespacedId HARVEST_LUMBER = id("harvest_lumber");
    public static final NamespacedId CLEAN_CUTS = id("clean_cuts");
    public static final NamespacedId KNOCK_ON_WOOD = id("knock_on_wood");
    public static final NamespacedId LEAF_BLOWER = id("leaf_blower");

    private CoreWoodcuttingAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerActive(new ActiveAbilityDefinition(
                TREE_FELLER,
                CoreSkills.WOODCUTTING,
                5,
                Duration.ofSeconds(4),
                Duration.ofSeconds(2),
                Duration.ofSeconds(240),
                Map.of(
                        "upstream", "SuperAbilityType.TREE_FELLER",
                        "runtimeUnlock", "skillranks.yml",
                        "durationFormula", "2 + level/increase")));
        registrar.registerPassive(passive(
                HARVEST_LUMBER, 1, "SubSkillType.WOODCUTTING_HARVEST_LUMBER"));
        registrar.registerPassive(passive(
                CLEAN_CUTS, 100, "SubSkillType.WOODCUTTING_CLEAN_CUTS"));
        registrar.registerPassive(passive(
                KNOCK_ON_WOOD, 30, "SubSkillType.WOODCUTTING_KNOCK_ON_WOOD"));
        registrar.registerPassive(passive(
                LEAF_BLOWER, 15, "SubSkillType.WOODCUTTING_LEAF_BLOWER"));
    }

    private static PassiveDefinition passive(NamespacedId id, int unlock, String upstream) {
        return new PassiveDefinition(
                id,
                CoreSkills.WOODCUTTING,
                unlock,
                Map.of("upstream", upstream, "runtimeUnlock", "skillranks.yml"));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", path);
    }
}

package io.github.njw3995.fabricmmo.core.skill.archery;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;

/** Public API definitions for mcMMO 2.3.000 Archery passives. */
public final class CoreArcheryAbilities {
    public static final NamespacedId ARROW_RETRIEVAL = id("arrow_retrieval");
    public static final NamespacedId DAZE = id("daze");
    public static final NamespacedId SKILL_SHOT = id("skill_shot");
    public static final NamespacedId LIMIT_BREAK = id("limit_break");

    private CoreArcheryAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerPassive(new PassiveDefinition(
                ARROW_RETRIEVAL, CoreSkills.ARCHERY, 1,
                Map.of("upstream", "SubSkillType.ARCHERY_ARROW_RETRIEVAL")));
        registrar.registerPassive(new PassiveDefinition(
                DAZE, CoreSkills.ARCHERY, 0,
                Map.of("upstream", "SubSkillType.ARCHERY_DAZE")));
        registrar.registerPassive(new PassiveDefinition(
                SKILL_SHOT, CoreSkills.ARCHERY, 1,
                Map.of("upstream", "SubSkillType.ARCHERY_SKILL_SHOT")));
        registrar.registerPassive(new PassiveDefinition(
                LIMIT_BREAK, CoreSkills.ARCHERY, 100,
                Map.of("upstream", "SubSkillType.ARCHERY_ARCHERY_LIMIT_BREAK")));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", "archery_" + path);
    }
}

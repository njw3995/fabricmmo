package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;

/** Public API definitions for the mcMMO 2.3.000 Acrobatics subskill family. */
public final class CoreAcrobaticsAbilities {
    public static final NamespacedId ROLL = id("roll");
    public static final NamespacedId DODGE = id("dodge");

    private CoreAcrobaticsAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerPassive(new PassiveDefinition(
                ROLL,
                CoreSkills.ACROBATICS,
                0,
                Map.of("upstream", "SubSkillType.ACROBATICS_ROLL", "runtimeUnlock", "rankless")));
        registrar.registerPassive(new PassiveDefinition(
                DODGE,
                CoreSkills.ACROBATICS,
                1,
                Map.of("upstream", "SubSkillType.ACROBATICS_DODGE", "runtimeUnlock", "skillranks.yml")));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", "acrobatics_" + path);
    }
}

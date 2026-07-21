package io.github.njw3995.fabricmmo.core.skill.maces;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;

/** Public API definitions for the mcMMO 2.3.000 Maces subskill family. */
public final class CoreMacesAbilities {
    public static final NamespacedId CRIPPLE = id("cripple");
    public static final NamespacedId CRUSH = id("crush");
    public static final NamespacedId LIMIT_BREAK = id("limit_break");

    private CoreMacesAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerPassive(new PassiveDefinition(
                LIMIT_BREAK, CoreSkills.MACES, 100,
                Map.of("upstream", "SubSkillType.MACES_MACES_LIMIT_BREAK")));
        registrar.registerPassive(new PassiveDefinition(
                CRUSH, CoreSkills.MACES, 100,
                Map.of("upstream", "SubSkillType.MACES_CRUSH")));
        registrar.registerPassive(new PassiveDefinition(
                CRIPPLE, CoreSkills.MACES, 50,
                Map.of("upstream", "SubSkillType.MACES_CRIPPLE")));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", "maces_" + path);
    }
}

package io.github.njw3995.fabricmmo.core.skill.crossbows;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;

/** Public API definitions for mcMMO 2.3.000 Crossbows passives. */
public final class CoreCrossbowsAbilities {
    public static final NamespacedId POWERED_SHOT = id("powered_shot");
    public static final NamespacedId TRICK_SHOT = id("trick_shot");
    public static final NamespacedId LIMIT_BREAK = id("limit_break");

    private CoreCrossbowsAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerPassive(new PassiveDefinition(
                POWERED_SHOT, CoreSkills.CROSSBOWS, 1,
                Map.of("upstream", "SubSkillType.CROSSBOWS_POWERED_SHOT")));
        registrar.registerPassive(new PassiveDefinition(
                TRICK_SHOT, CoreSkills.CROSSBOWS, 50,
                Map.of("upstream", "SubSkillType.CROSSBOWS_TRICK_SHOT")));
        registrar.registerPassive(new PassiveDefinition(
                LIMIT_BREAK, CoreSkills.CROSSBOWS, 100,
                Map.of("upstream", "SubSkillType.CROSSBOWS_CROSSBOWS_LIMIT_BREAK")));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", "crossbows_" + path);
    }
}

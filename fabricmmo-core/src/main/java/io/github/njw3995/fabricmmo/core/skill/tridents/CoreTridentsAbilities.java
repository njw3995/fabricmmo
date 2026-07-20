package io.github.njw3995.fabricmmo.core.skill.tridents;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;

/** Public API definitions for mcMMO 2.3.000 Tridents passives. */
public final class CoreTridentsAbilities {
    public static final NamespacedId IMPALE = id("impale");
    public static final NamespacedId LIMIT_BREAK = id("limit_break");

    private CoreTridentsAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerPassive(new PassiveDefinition(
                IMPALE, CoreSkills.TRIDENTS, 50,
                Map.of("upstream", "SubSkillType.TRIDENTS_IMPALE")));
        registrar.registerPassive(new PassiveDefinition(
                LIMIT_BREAK, CoreSkills.TRIDENTS, 100,
                Map.of("upstream", "SubSkillType.TRIDENTS_TRIDENTS_LIMIT_BREAK")));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", "tridents_" + path);
    }
}

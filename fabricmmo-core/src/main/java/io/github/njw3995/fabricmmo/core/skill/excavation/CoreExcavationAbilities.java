package io.github.njw3995.fabricmmo.core.skill.excavation;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.time.Duration;
import java.util.Map;

/** Public API definitions for the mcMMO 2.3.000 Excavation ability family. */
public final class CoreExcavationAbilities {
    public static final NamespacedId GIGA_DRILL_BREAKER = id("giga_drill_breaker");
    public static final NamespacedId ARCHAEOLOGY = id("archaeology");

    private CoreExcavationAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerActive(new ActiveAbilityDefinition(
                GIGA_DRILL_BREAKER,
                CoreSkills.EXCAVATION,
                5,
                Duration.ofSeconds(4),
                Duration.ofSeconds(2),
                Duration.ofSeconds(240),
                Map.of(
                        "upstream", "SuperAbilityType.GIGA_DRILL_BREAKER",
                        "runtimeUnlock", "skillranks.yml",
                        "durationFormula", "2 + level/increase")));
        registrar.registerPassive(new PassiveDefinition(
                ARCHAEOLOGY,
                CoreSkills.EXCAVATION,
                1,
                Map.of(
                        "upstream", "SubSkillType.EXCAVATION_ARCHAEOLOGY",
                        "runtimeUnlock", "skillranks.yml")));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", path);
    }
}

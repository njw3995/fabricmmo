package io.github.njw3995.fabricmmo.core.skill.axes;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.time.Duration;
import java.util.Map;

/** Public API definitions for the mcMMO 2.3.000 Axes subskill family. */
public final class CoreAxesAbilities {
    public static final NamespacedId SKULL_SPLITTER = id("skull_splitter");
    public static final NamespacedId CRITICAL_STRIKES = id("critical_strikes");
    public static final NamespacedId AXE_MASTERY = id("axe_mastery");
    public static final NamespacedId ARMOR_IMPACT = id("armor_impact");
    public static final NamespacedId GREATER_IMPACT = id("greater_impact");
    public static final NamespacedId LIMIT_BREAK = id("limit_break");

    private CoreAxesAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerActive(new ActiveAbilityDefinition(
                SKULL_SPLITTER,
                CoreSkills.AXES,
                50,
                Duration.ofSeconds(4),
                Duration.ofSeconds(2),
                Duration.ofSeconds(240),
                Map.of(
                        "upstream", "SuperAbilityType.SKULL_SPLITTER",
                        "runtimeUnlock", "skillranks.yml",
                        "durationFormula", "2 + level/increase")));
        registrar.registerPassive(new PassiveDefinition(
                CRITICAL_STRIKES, CoreSkills.AXES, 1,
                Map.of("upstream", "SubSkillType.AXES_CRITICAL_STRIKES")));
        registrar.registerPassive(new PassiveDefinition(
                AXE_MASTERY, CoreSkills.AXES, 50,
                Map.of("upstream", "SubSkillType.AXES_AXE_MASTERY")));
        registrar.registerPassive(new PassiveDefinition(
                ARMOR_IMPACT, CoreSkills.AXES, 1,
                Map.of("upstream", "SubSkillType.AXES_ARMOR_IMPACT")));
        registrar.registerPassive(new PassiveDefinition(
                GREATER_IMPACT, CoreSkills.AXES, 250,
                Map.of("upstream", "SubSkillType.AXES_GREATER_IMPACT")));
        registrar.registerPassive(new PassiveDefinition(
                LIMIT_BREAK, CoreSkills.AXES, 100,
                Map.of("upstream", "SubSkillType.AXES_AXES_LIMIT_BREAK")));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", "axes_" + path);
    }
}

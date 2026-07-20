package io.github.njw3995.fabricmmo.core.skill.swords;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.time.Duration;
import java.util.Map;

/** Public API definitions for the mcMMO 2.3.000 Swords subskill family. */
public final class CoreSwordsAbilities {
    public static final NamespacedId SERRATED_STRIKES = id("serrated_strikes");
    public static final NamespacedId COUNTER_ATTACK = id("counter_attack");
    public static final NamespacedId RUPTURE = id("rupture");
    public static final NamespacedId STAB = id("stab");
    public static final NamespacedId LIMIT_BREAK = id("limit_break");

    private CoreSwordsAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerActive(new ActiveAbilityDefinition(
                SERRATED_STRIKES,
                CoreSkills.SWORDS,
                50,
                Duration.ofSeconds(4),
                Duration.ofSeconds(2),
                Duration.ofSeconds(240),
                Map.of(
                        "upstream", "SuperAbilityType.SERRATED_STRIKES",
                        "runtimeUnlock", "skillranks.yml",
                        "durationFormula", "2 + level/increase")));
        registrar.registerPassive(new PassiveDefinition(
                COUNTER_ATTACK, CoreSkills.SWORDS, 200,
                Map.of("upstream", "SubSkillType.SWORDS_COUNTER_ATTACK")));
        registrar.registerPassive(new PassiveDefinition(
                RUPTURE, CoreSkills.SWORDS, 1,
                Map.of("upstream", "SubSkillType.SWORDS_RUPTURE")));
        registrar.registerPassive(new PassiveDefinition(
                STAB, CoreSkills.SWORDS, 750,
                Map.of("upstream", "SubSkillType.SWORDS_STAB")));
        registrar.registerPassive(new PassiveDefinition(
                LIMIT_BREAK, CoreSkills.SWORDS, 100,
                Map.of("upstream", "SubSkillType.SWORDS_SWORDS_LIMIT_BREAK")));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", "swords_" + path);
    }
}

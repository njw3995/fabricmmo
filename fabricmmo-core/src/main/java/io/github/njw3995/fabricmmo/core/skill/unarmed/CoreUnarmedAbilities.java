package io.github.njw3995.fabricmmo.core.skill.unarmed;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.time.Duration;
import java.util.Map;

/** Public API definitions for the mcMMO 2.3.000 Unarmed subskill family. */
public final class CoreUnarmedAbilities {
    public static final NamespacedId BERSERK = id("berserk");
    public static final NamespacedId ARROW_DEFLECT = id("arrow_deflect");
    public static final NamespacedId DISARM = id("disarm");
    public static final NamespacedId IRON_GRIP = id("iron_grip");
    public static final NamespacedId STEEL_ARM_STYLE = id("steel_arm_style");
    public static final NamespacedId BLOCK_CRACKER = id("block_cracker");
    public static final NamespacedId LIMIT_BREAK = id("limit_break");

    private CoreUnarmedAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerActive(new ActiveAbilityDefinition(
                BERSERK,
                CoreSkills.UNARMED,
                50,
                Duration.ofSeconds(4),
                Duration.ofSeconds(2),
                Duration.ofSeconds(240),
                Map.of(
                        "upstream", "SuperAbilityType.BERSERK",
                        "runtimeUnlock", "skillranks.yml",
                        "durationFormula", "2 + level/increase")));
        registrar.registerPassive(passive(
                ARROW_DEFLECT, 200, "SubSkillType.UNARMED_ARROW_DEFLECT"));
        registrar.registerPassive(passive(DISARM, 250, "SubSkillType.UNARMED_DISARM"));
        registrar.registerPassive(passive(IRON_GRIP, 600, "SubSkillType.UNARMED_IRON_GRIP"));
        registrar.registerPassive(passive(
                STEEL_ARM_STYLE, 1, "SubSkillType.UNARMED_STEEL_ARM_STYLE"));
        registrar.registerPassive(passive(
                BLOCK_CRACKER, 0, "SubSkillType.UNARMED_BLOCK_CRACKER"));
        registrar.registerPassive(passive(
                LIMIT_BREAK, 100, "SubSkillType.UNARMED_UNARMED_LIMIT_BREAK"));
    }

    private static PassiveDefinition passive(
            NamespacedId id, int unlock, String upstream) {
        return new PassiveDefinition(id, CoreSkills.UNARMED, unlock, Map.of("upstream", upstream));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", "unarmed_" + path);
    }
}

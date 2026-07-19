package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.time.Duration;
import java.util.Map;

/** Public API definitions for the mcMMO 2.3.000 Herbalism ability family. */
public final class CoreHerbalismAbilities {
    public static final NamespacedId GREEN_TERRA = id("green_terra");
    public static final NamespacedId DOUBLE_DROPS = id("herbalism_double_drops");
    public static final NamespacedId VERDANT_BOUNTY = id("verdant_bounty");
    public static final NamespacedId GREEN_THUMB = id("green_thumb");
    public static final NamespacedId FARMERS_DIET = id("farmers_diet");
    public static final NamespacedId HYLIAN_LUCK = id("hylian_luck");
    public static final NamespacedId SHROOM_THUMB = id("shroom_thumb");

    private CoreHerbalismAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerActive(new ActiveAbilityDefinition(
                GREEN_TERRA,
                CoreSkills.HERBALISM,
                5,
                Duration.ofSeconds(4),
                Duration.ofSeconds(2),
                Duration.ofSeconds(240),
                Map.of(
                        "upstream", "SuperAbilityType.GREEN_TERRA",
                        "runtimeUnlock", "skillranks.yml",
                        "durationFormula", "2 + level/increase")));
        passive(registrar, DOUBLE_DROPS, 1, "SubSkillType.HERBALISM_DOUBLE_DROPS");
        passive(registrar, VERDANT_BOUNTY, 100, "SubSkillType.HERBALISM_VERDANT_BOUNTY");
        passive(registrar, GREEN_THUMB, 25, "SubSkillType.HERBALISM_GREEN_THUMB");
        passive(registrar, FARMERS_DIET, 20, "SubSkillType.HERBALISM_FARMERS_DIET");
        passive(registrar, HYLIAN_LUCK, 0, "SubSkillType.HERBALISM_HYLIAN_LUCK");
        passive(registrar, SHROOM_THUMB, 0, "SubSkillType.HERBALISM_SHROOM_THUMB");
    }

    private static void passive(
            AbilityRegistrar registrar,
            NamespacedId id,
            int unlock,
            String upstream) {
        registrar.registerPassive(new PassiveDefinition(
                id,
                CoreSkills.HERBALISM,
                unlock,
                Map.of("upstream", upstream, "runtimeUnlock", "skillranks.yml")));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", path);
    }
}

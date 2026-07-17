package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.time.Duration;
import java.util.Map;

/** Public API definitions for the complete Mining ability family. */
public final class CoreMiningAbilities {
    public static final NamespacedId SUPER_BREAKER = id("super_breaker");
    public static final NamespacedId BLAST_MINING = id("blast_mining");
    public static final NamespacedId DOUBLE_DROPS = id("double_drops");
    public static final NamespacedId MOTHER_LODE = id("mother_lode");
    public static final NamespacedId BIGGER_BOMBS = id("bigger_bombs");
    public static final NamespacedId DEMOLITIONS_EXPERTISE = id("demolitions_expertise");

    private CoreMiningAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerActive(new ActiveAbilityDefinition(
                SUPER_BREAKER,
                CoreSkills.MINING,
                0,
                Duration.ofSeconds(4),
                Duration.ofSeconds(2),
                Duration.ofSeconds(240),
                Map.of(
                        "upstream", "SuperAbilityType.SUPER_BREAKER",
                        "runtimeUnlock", "skillranks.yml",
                        "durationFormula", "2 + level/increase")));
        registrar.registerActive(new ActiveAbilityDefinition(
                BLAST_MINING,
                CoreSkills.MINING,
                10,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(60),
                Map.of(
                        "upstream", "SuperAbilityType.BLAST_MINING",
                        "instantaneous", "true",
                        "runtimeUnlock", "skillranks.yml")));
        registrar.registerPassive(passive(
                DOUBLE_DROPS, 0, "SubSkillType.MINING_DOUBLE_DROPS"));
        registrar.registerPassive(passive(
                MOTHER_LODE, 0, "SubSkillType.MINING_MOTHER_LODE"));
        registrar.registerPassive(passive(
                BIGGER_BOMBS, 10, "SubSkillType.MINING_BIGGER_BOMBS"));
        registrar.registerPassive(passive(
                DEMOLITIONS_EXPERTISE, 50,
                "SubSkillType.MINING_DEMOLITIONS_EXPERTISE"));
    }

    private static PassiveDefinition passive(NamespacedId id, int unlock, String upstream) {
        return new PassiveDefinition(
                id,
                CoreSkills.MINING,
                unlock,
                Map.of("upstream", upstream, "runtimeUnlock", "skillranks.yml"));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", path);
    }
}

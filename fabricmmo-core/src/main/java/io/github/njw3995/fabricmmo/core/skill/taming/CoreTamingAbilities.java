package io.github.njw3995.fabricmmo.core.skill.taming;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;

/** Public API definitions for the mcMMO 2.3.000 Taming subskills. */
public final class CoreTamingAbilities {
    public static final NamespacedId BEAST_LORE = id("beast_lore");
    public static final NamespacedId CALL_OF_THE_WILD = id("call_of_the_wild");
    public static final NamespacedId GORE = id("gore");
    public static final NamespacedId SHARPENED_CLAWS = id("sharpened_claws");
    public static final NamespacedId ENVIRONMENTALLY_AWARE = id("environmentally_aware");
    public static final NamespacedId THICK_FUR = id("thick_fur");
    public static final NamespacedId HOLY_HOUND = id("holy_hound");
    public static final NamespacedId SHOCK_PROOF = id("shock_proof");
    public static final NamespacedId FAST_FOOD_SERVICE = id("fast_food_service");
    public static final NamespacedId PUMMEL = id("pummel");

    private CoreTamingAbilities() {}

    public static void registerAll(AbilityRegistrar registrar) {
        passive(registrar, BEAST_LORE, 1, "TamingManager#beastLore");
        passive(registrar, CALL_OF_THE_WILD, 1, "TamingManager#summon");
        passive(registrar, GORE, 15, "CombatUtils#processTamingCombat");
        passive(registrar, SHARPENED_CLAWS, 75, "CombatUtils#processTamingCombat");
        passive(registrar, ENVIRONMENTALLY_AWARE, 10, "TamingManager#processEnvironmentallyAware");
        passive(registrar, THICK_FUR, 25, "TamingManager#processThickFur");
        passive(registrar, HOLY_HOUND, 35, "TamingManager#processHolyHound");
        passive(registrar, SHOCK_PROOF, 50, "TamingManager#processShockProof");
        passive(registrar, FAST_FOOD_SERVICE, 20, "CombatUtils#processTamingCombat");
        passive(registrar, PUMMEL, 20, "CombatUtils#processTamingCombat");
    }

    private static void passive(AbilityRegistrar registrar, NamespacedId id, int unlock,
                                String upstream) {
        registrar.registerPassive(new PassiveDefinition(id, CoreSkills.TAMING, unlock,
                Map.of("upstream", upstream, "runtimeUnlock", "skillranks.yml")));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", "taming_" + path);
    }
}

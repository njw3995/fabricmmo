package io.github.njw3995.fabricmmo.core.skill.fishing;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;

/** Public API definitions for the mcMMO 2.3.000 Fishing subskill family. */
public final class CoreFishingAbilities {
    public static final NamespacedId TREASURE_HUNTER = id("treasure_hunter");
    public static final NamespacedId MAGIC_HUNTER = id("magic_hunter");
    public static final NamespacedId SHAKE = id("shake");
    public static final NamespacedId FISHERMANS_DIET = id("fishermans_diet");
    public static final NamespacedId MASTER_ANGLER = id("master_angler");
    public static final NamespacedId ICE_FISHING = id("ice_fishing");
    public static final NamespacedId VANILLA_XP_BOOST = id("vanilla_xp_boost");

    private CoreFishingAbilities() {
    }

    public static void registerAll(AbilityRegistrar registrar) {
        passive(registrar, TREASURE_HUNTER, 1, "SubSkillType.FISHING_TREASURE_HUNTER");
        passive(registrar, MAGIC_HUNTER, 20, "SubSkillType.FISHING_MAGIC_HUNTER");
        passive(registrar, SHAKE, 15, "SubSkillType.FISHING_SHAKE");
        passive(registrar, FISHERMANS_DIET, 20, "SubSkillType.FISHING_FISHERMANS_DIET");
        passive(registrar, MASTER_ANGLER, 1, "SubSkillType.FISHING_MASTER_ANGLER");
        passive(registrar, ICE_FISHING, 5, "SubSkillType.FISHING_ICE_FISHING");
        passive(registrar, VANILLA_XP_BOOST, 1, "FishingManager#getVanillaXPBoostModifier");
    }

    private static void passive(
            AbilityRegistrar registrar,
            NamespacedId id,
            int unlock,
            String upstream) {
        registrar.registerPassive(new PassiveDefinition(
                id,
                CoreSkills.FISHING,
                unlock,
                Map.of("upstream", upstream, "runtimeUnlock", "skillranks.yml")));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", "fishing_" + path);
    }
}

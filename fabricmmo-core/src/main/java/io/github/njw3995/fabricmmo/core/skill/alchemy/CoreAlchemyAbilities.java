package io.github.njw3995.fabricmmo.core.skill.alchemy;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;

public final class CoreAlchemyAbilities {
    public static final NamespacedId CATALYSIS = new NamespacedId("fabricmmo", "alchemy_catalysis");
    public static final NamespacedId CONCOCTIONS = new NamespacedId("fabricmmo", "alchemy_concoctions");
    private CoreAlchemyAbilities() {}
    public static void registerAll(AbilityRegistrar registrar) {
        registrar.registerPassive(new PassiveDefinition(CATALYSIS, CoreSkills.ALCHEMY, 0,
                Map.of("upstream", "AlchemyManager#calculateBrewSpeed", "runtimeUnlock", "skillranks.yml")));
        registrar.registerPassive(new PassiveDefinition(CONCOCTIONS, CoreSkills.ALCHEMY, 0,
                Map.of("upstream", "PotionConfig#getIngredients", "runtimeUnlock", "skillranks.yml")));
    }
}

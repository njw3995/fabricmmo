package io.github.njw3995.fabricmmo.core.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.XpSourceDefinition;
import io.github.njw3995.fabricmmo.api.progression.XpSourceRegistrar;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;

public final class CoreXpSources {
    public static final NamespacedId MINING_BLOCK_BREAK = NamespacedId.parse("fabricmmo:mining_block_break");

    private CoreXpSources() {
    }

    public static void registerDefaults(XpSourceRegistrar registrar) {
        registrar.registerXpSource(new XpSourceDefinition(
                MINING_BLOCK_BREAK,
                CoreSkills.MINING,
                Map.of("upstream", "MiningManager#miningBlockCheck")));
    }
}

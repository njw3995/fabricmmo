package io.github.njw3995.fabricmmo.core.skill.ranged;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.event.PassiveActivationEvent;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import java.util.UUID;
import net.minecraft.util.math.random.Random;

/** Publishes the API chance event before resolving an mcMMO passive roll. */
public final class RangedPassiveEvents {
    private RangedPassiveEvents() {
    }

    public static boolean roll(
            UUID playerId,
            NamespacedId passiveId,
            double chancePercent,
            Random random) {
        double probability = Math.clamp(chancePercent / 100.0D, 0.0D, 1.0D);
        PassiveActivationEvent event = FabricMmoFabricRuntime.requireApi().events().publish(
                new PassiveActivationEvent(playerId, passiveId, probability));
        return !event.cancelled() && random.nextDouble() < event.resultingProbability();
    }
}

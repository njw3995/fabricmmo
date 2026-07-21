package io.github.njw3995.fabricmmo.api.ability;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.random.RandomSource;
import java.util.UUID;

/** Resolves registered passives through unlock checks and cancellable FabricMMO events. */
public interface PassiveService {
    /**
     * Preferred gameplay entry point. Core also validates online presence, game mode, and skill or
     * passive permissions before resolving the chance.
     */
    PassiveResult rollOnline(
            UUID playerId,
            NamespacedId passiveId,
            double baseProbability,
            RandomSource random);

    /**
     * Low-level resolver for controlled tests and non-player contexts. Normal gameplay listeners
     * should use {@link #rollOnline(UUID, NamespacedId, double, RandomSource)}.
     */
    PassiveResult roll(
            UUID playerId,
            NamespacedId passiveId,
            double baseProbability,
            RandomSource random);

    static PassiveService unsupported() {
        return new PassiveService() {
            private UnsupportedOperationException unsupported() {
                return new UnsupportedOperationException(
                        "This FabricMMO API implementation does not provide passive resolution");
            }

            @Override
            public PassiveResult rollOnline(
                    UUID playerId,
                    NamespacedId passiveId,
                    double baseProbability,
                    RandomSource random) {
                throw unsupported();
            }

            @Override
            public PassiveResult roll(
                    UUID playerId,
                    NamespacedId passiveId,
                    double baseProbability,
                    RandomSource random) {
                throw unsupported();
            }
        };
    }
}

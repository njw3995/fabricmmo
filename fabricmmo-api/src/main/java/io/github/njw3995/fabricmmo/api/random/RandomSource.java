package io.github.njw3995.fabricmmo.api.random;

public interface RandomSource {
    double nextDouble();

    default boolean roll(double probability) {
        if (!Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("probability must be in [0,1]");
        }
        return nextDouble() < probability;
    }
}

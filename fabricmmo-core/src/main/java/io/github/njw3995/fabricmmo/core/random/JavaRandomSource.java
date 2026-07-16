package io.github.njw3995.fabricmmo.core.random;

import io.github.njw3995.fabricmmo.api.random.RandomSource;
import java.util.random.RandomGenerator;

public final class JavaRandomSource implements RandomSource {
    private final RandomGenerator generator;

    public JavaRandomSource(RandomGenerator generator) {
        this.generator = generator;
    }

    @Override
    public double nextDouble() {
        return generator.nextDouble();
    }
}

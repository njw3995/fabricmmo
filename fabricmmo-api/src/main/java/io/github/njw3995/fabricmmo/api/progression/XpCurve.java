package io.github.njw3995.fabricmmo.api.progression;

public record XpCurve(int linearBase, double linearMultiplier, int exponentialBase,
                      double exponentialMultiplier, double exponentialExponent) {
    public XpCurve {
        if (linearBase < 0 || exponentialBase < 0) {
            throw new IllegalArgumentException("Formula bases must be non-negative");
        }
        if (!(linearMultiplier > 0.0) || !(exponentialMultiplier > 0.0)
                || !(exponentialExponent > 0.0)) {
            throw new IllegalArgumentException("Formula multipliers and exponent must be positive");
        }
    }

    public static XpCurve upstreamDefaults() {
        return new XpCurve(1020, 20.0, 2000, 0.1, 1.80);
    }
}

package io.github.njw3995.fabricmmo.api.ability;

public record PassiveResult(Status status, double probability) {
    public enum Status {
        INELIGIBLE,
        ACTIVATED,
        FAILED,
        LOCKED,
        CANCELLED
    }

    public PassiveResult {
        if (!Double.isFinite(probability) || probability < 0.0D || probability > 1.0D) {
            throw new IllegalArgumentException("probability must be between 0 and 1");
        }
    }

    public boolean activated() {
        return status == Status.ACTIVATED;
    }
}

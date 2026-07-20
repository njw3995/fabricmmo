package io.github.njw3995.fabricmmo.api.event;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Objects;
import java.util.UUID;

/**
 * Fired immediately before a server-authoritative passive chance is resolved.
 * Addons may cancel the activation or multiply its final probability.
 */
public final class PassiveActivationEvent implements CancellableEvent {
    private final UUID playerId;
    private final NamespacedId passiveId;
    private final double baseProbability;
    private double resultMultiplier = 1.0D;
    private boolean cancelled;

    public PassiveActivationEvent(
            UUID playerId,
            NamespacedId passiveId,
            double baseProbability) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.passiveId = Objects.requireNonNull(passiveId, "passiveId");
        if (!Double.isFinite(baseProbability)
                || baseProbability < 0.0D
                || baseProbability > 1.0D) {
            throw new IllegalArgumentException("baseProbability must be between 0 and 1");
        }
        this.baseProbability = baseProbability;
    }

    public UUID playerId() {
        return playerId;
    }

    public NamespacedId passiveId() {
        return passiveId;
    }

    public double baseProbability() {
        return baseProbability;
    }

    public double resultMultiplier() {
        return resultMultiplier;
    }

    public void resultMultiplier(double resultMultiplier) {
        if (!Double.isFinite(resultMultiplier) || resultMultiplier < 0.0D) {
            throw new IllegalArgumentException(
                    "resultMultiplier must be finite and non-negative");
        }
        this.resultMultiplier = resultMultiplier;
    }

    public double resultingProbability() {
        return Math.min(1.0D, baseProbability * resultMultiplier);
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}

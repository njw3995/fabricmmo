package io.github.njw3995.fabricmmo.api.event;

import java.util.Objects;
import java.util.UUID;

/** Published when a player-owned brewing stand begins an Alchemy brew. */
public final class AlchemyCatalysisEvent implements CancellableEvent {
    private final UUID playerId;
    private final String dimensionId;
    private final long blockPosition;
    private double speed;
    private boolean cancelled;

    public AlchemyCatalysisEvent(UUID playerId, String dimensionId, long blockPosition,
                                 double speed) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
        this.blockPosition = blockPosition;
        setSpeed(speed);
    }

    public UUID playerId() { return playerId; }
    public String dimensionId() { return dimensionId; }
    public long blockPosition() { return blockPosition; }
    public double speed() { return speed; }

    public void setSpeed(double speed) {
        if (!Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException("speed must be finite and positive");
        }
        this.speed = speed;
    }

    @Override public boolean cancelled() { return cancelled; }
    @Override public void cancel() { cancelled = true; }
}

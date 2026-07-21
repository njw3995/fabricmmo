package io.github.njw3995.fabricmmo.api.event;

import java.util.Objects;
import java.util.UUID;

/** Published before FabricMMO awards Taming XP for a newly tamed vanilla animal. */
public final class TamingEntityTamedEvent implements CancellableEvent {
    private final UUID playerId;
    private final UUID entityId;
    private final String entityType;
    private double xp;
    private boolean cancelled;

    public TamingEntityTamedEvent(UUID playerId, UUID entityId, String entityType, double xp) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.entityId = Objects.requireNonNull(entityId, "entityId");
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        setXp(xp);
    }

    public UUID playerId() { return playerId; }
    public UUID entityId() { return entityId; }
    public String entityType() { return entityType; }
    public double xp() { return xp; }

    public void setXp(double xp) {
        if (!Double.isFinite(xp) || xp < 0.0D) {
            throw new IllegalArgumentException("xp must be finite and non-negative");
        }
        this.xp = xp;
    }

    @Override public boolean cancelled() { return cancelled; }
    @Override public void cancel() { cancelled = true; }
}

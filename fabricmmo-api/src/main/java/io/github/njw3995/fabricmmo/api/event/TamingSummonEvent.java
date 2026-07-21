package io.github.njw3995.fabricmmo.api.event;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/** Published before a Call of the Wild summon is added to the world. */
public final class TamingSummonEvent implements CancellableEvent {
    private final UUID playerId;
    private final UUID entityId;
    private final String summonType;
    private final Duration lifetime;
    private boolean cancelled;

    public TamingSummonEvent(UUID playerId, UUID entityId, String summonType, Duration lifetime) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.entityId = Objects.requireNonNull(entityId, "entityId");
        this.summonType = Objects.requireNonNull(summonType, "summonType");
        this.lifetime = Objects.requireNonNull(lifetime, "lifetime");
        if (lifetime.isNegative()) throw new IllegalArgumentException("lifetime must not be negative");
    }

    public UUID playerId() { return playerId; }
    public UUID entityId() { return entityId; }
    public String summonType() { return summonType; }
    public Duration lifetime() { return lifetime; }
    @Override public boolean cancelled() { return cancelled; }
    @Override public void cancel() { cancelled = true; }
}

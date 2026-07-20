package io.github.njw3995.fabricmmo.api.event;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Published immediately before FabricMMO commits a successful custom Alchemy brew. */
public final class AlchemyBrewEvent implements CancellableEvent {
    private final UUID playerId;
    private final String dimensionId;
    private final long blockPosition;
    private final String ingredientId;
    private final List<String> outputPotionIds;
    private boolean cancelled;

    public AlchemyBrewEvent(UUID playerId, String dimensionId, long blockPosition,
                            String ingredientId, List<String> outputPotionIds) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
        this.blockPosition = blockPosition;
        this.ingredientId = Objects.requireNonNull(ingredientId, "ingredientId");
        this.outputPotionIds = List.copyOf(outputPotionIds);
    }

    public UUID playerId() { return playerId; }
    public String dimensionId() { return dimensionId; }
    public long blockPosition() { return blockPosition; }
    public String ingredientId() { return ingredientId; }
    public List<String> outputPotionIds() { return outputPotionIds; }
    @Override public boolean cancelled() { return cancelled; }
    @Override public void cancel() { cancelled = true; }
}

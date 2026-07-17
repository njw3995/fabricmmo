package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Objects;

public record MiningDropStack(NamespacedId itemId, int count, int maxCount, boolean blockItem) {
    public MiningDropStack {
        Objects.requireNonNull(itemId, "itemId");
        if (count < 0 || maxCount <= 0) {
            throw new IllegalArgumentException("Invalid drop stack size");
        }
    }

    public MiningDropStack withCount(int newCount) {
        return new MiningDropStack(itemId, newCount, maxCount, blockItem);
    }
}

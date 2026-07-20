package io.github.njw3995.fabricmmo.core.skill.taming;

import java.util.Objects;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public record TamingSummonSettings(Identifier itemId, int itemAmount, int summonAmount,
                                   int summonLengthSeconds, int perPlayerLimit) {
    public TamingSummonSettings {
        Objects.requireNonNull(itemId, "itemId");
        if (itemAmount < 0 || summonAmount < 1 || summonLengthSeconds < 0 || perPlayerLimit < 1) {
            throw new IllegalArgumentException("Invalid Call of the Wild settings");
        }
    }

    public Item item() {
        return Registries.ITEM.get(itemId);
    }
}

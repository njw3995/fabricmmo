package io.github.njw3995.fabricmmo.core.skill.alchemy;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;

/** Persistent owner and in-flight brew state stored directly on a brewing stand. */
public record AlchemyBrewState(UUID owner, double remainingTicks, double speed, String ingredientId) {
    private static final String OWNER = "FabricMMOAlchemyOwner";
    private static final String REMAINING = "FabricMMOAlchemyRemaining";
    private static final String SPEED = "FabricMMOAlchemySpeed";
    private static final String INGREDIENT = "FabricMMOAlchemyIngredient";

    public AlchemyBrewState {
        ingredientId = ingredientId == null ? "" : ingredientId;
        if (!Double.isFinite(remainingTicks) || remainingTicks < 0.0D) {
            throw new IllegalArgumentException("remainingTicks must be finite and non-negative");
        }
        if (!Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException("speed must be finite and positive");
        }
    }

    public boolean active() {
        return remainingTicks > 0.0D && !ingredientId.isBlank();
    }

    public void write(NbtCompound nbt) {
        Objects.requireNonNull(nbt, "nbt");
        if (owner != null) nbt.putUuid(OWNER, owner);
        if (active()) {
            nbt.putDouble(REMAINING, remainingTicks);
            nbt.putDouble(SPEED, speed);
            nbt.putString(INGREDIENT, ingredientId);
        }
    }

    public static AlchemyBrewState read(NbtCompound nbt) {
        Objects.requireNonNull(nbt, "nbt");
        UUID owner = nbt.containsUuid(OWNER) ? nbt.getUuid(OWNER) : null;
        if (!nbt.contains(REMAINING)) return new AlchemyBrewState(owner, 0.0D, 1.0D, "");
        double remaining = Math.max(0.0D, nbt.getDouble(REMAINING));
        double speed = nbt.getDouble(SPEED);
        if (!Double.isFinite(speed) || speed <= 0.0D) speed = 1.0D;
        return new AlchemyBrewState(owner, remaining, speed, nbt.getString(INGREDIENT));
    }
}

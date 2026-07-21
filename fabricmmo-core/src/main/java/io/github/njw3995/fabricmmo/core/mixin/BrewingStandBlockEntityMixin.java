package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.access.AlchemyBrewingStandAccess;
import io.github.njw3995.fabricmmo.core.skill.alchemy.AlchemyBrewState;
import io.github.njw3995.fabricmmo.core.skill.alchemy.AlchemyRuntimeHandler;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlockEntity.class)
abstract class BrewingStandBlockEntityMixin implements AlchemyBrewingStandAccess {
    @Shadow private DefaultedList<ItemStack> inventory;
    @Shadow int brewTime;
    @Shadow int fuel;

    @Unique private UUID fabricmmo$alchemyOwner;
    @Unique private double fabricmmo$customAlchemyRemaining;
    @Unique private double fabricmmo$customAlchemySpeed = 1.0D;
    @Unique private String fabricmmo$customAlchemyIngredient = "";

    @Override public UUID fabricmmo$alchemyOwner() { return fabricmmo$alchemyOwner; }
    @Override public void fabricmmo$setAlchemyOwner(UUID owner) { fabricmmo$alchemyOwner = owner; }
    @Override public DefaultedList<ItemStack> fabricmmo$alchemyInventory() { return inventory; }
    @Override public int fabricmmo$alchemyBrewTime() { return brewTime; }
    @Override public void fabricmmo$setAlchemyBrewTime(int ticks) { brewTime = ticks; }
    @Override public int fabricmmo$alchemyFuel() { return fuel; }
    @Override public void fabricmmo$setAlchemyFuel(int value) { fuel = value; }
    @Override public boolean fabricmmo$customAlchemyActive() {
        return fabricmmo$customAlchemyRemaining > 0.0D
                && !fabricmmo$customAlchemyIngredient.isBlank();
    }
    @Override public double fabricmmo$customAlchemyRemaining() {
        return fabricmmo$customAlchemyRemaining;
    }
    @Override public double fabricmmo$customAlchemySpeed() { return fabricmmo$customAlchemySpeed; }
    @Override public String fabricmmo$customAlchemyIngredient() {
        return fabricmmo$customAlchemyIngredient;
    }
    @Override
    public void fabricmmo$beginCustomAlchemy(double remaining, double speed, String ingredientId) {
        fabricmmo$customAlchemyRemaining = remaining;
        fabricmmo$customAlchemySpeed = speed;
        fabricmmo$customAlchemyIngredient = ingredientId;
        brewTime = Math.max(1, (int) remaining);
    }
    @Override
    public void fabricmmo$setCustomAlchemyRemaining(double remaining) {
        fabricmmo$customAlchemyRemaining = Math.max(0.0D, remaining);
        brewTime = remaining <= 0.0D ? 0 : Math.max(1, (int) remaining);
    }
    @Override
    public void fabricmmo$clearCustomAlchemy() {
        fabricmmo$customAlchemyRemaining = 0.0D;
        fabricmmo$customAlchemySpeed = 1.0D;
        fabricmmo$customAlchemyIngredient = "";
        brewTime = 0;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private static void fabricmmo$tickAlchemy(
            World world,
            BlockPos pos,
            BlockState state,
            BrewingStandBlockEntity stand,
            CallbackInfo callback) {
        if (AlchemyRuntimeHandler.tick(world, pos, state, stand,
                (AlchemyBrewingStandAccess) stand)) {
            callback.cancel();
        }
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void fabricmmo$readAlchemyData(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registries,
            CallbackInfo callback) {
        AlchemyBrewState data = AlchemyBrewState.read(nbt);
        fabricmmo$alchemyOwner = data.owner();
        fabricmmo$customAlchemyRemaining = data.remainingTicks();
        fabricmmo$customAlchemySpeed = data.speed();
        fabricmmo$customAlchemyIngredient = data.ingredientId();
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void fabricmmo$writeAlchemyData(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registries,
            CallbackInfo callback) {
        new AlchemyBrewState(
                fabricmmo$alchemyOwner,
                fabricmmo$customAlchemyRemaining,
                fabricmmo$customAlchemySpeed,
                fabricmmo$customAlchemyIngredient).write(nbt);
    }

    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    private void fabricmmo$filterAlchemyHopperInsert(
            int slot,
            ItemStack stack,
            net.minecraft.util.math.Direction direction,
            CallbackInfoReturnable<Boolean> callback) {
        Boolean result = AlchemyRuntimeHandler.allowHopperInsert(slot, stack);
        if (result != null) callback.setReturnValue(result);
    }
}

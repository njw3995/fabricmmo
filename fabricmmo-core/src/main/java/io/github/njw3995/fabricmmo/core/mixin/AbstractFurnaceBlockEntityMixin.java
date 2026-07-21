package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.smelting.OwnedProcessingBlock;
import io.github.njw3995.fabricmmo.core.skill.smelting.ProcessingBlockOwnershipNbt;
import io.github.njw3995.fabricmmo.core.skill.smelting.SmeltingCraftContext;
import io.github.njw3995.fabricmmo.core.skill.smelting.SmeltingRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.smelting.SmeltingVanillaXpContext;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
abstract class AbstractFurnaceBlockEntityMixin implements OwnedProcessingBlock {
    @Unique
    private UUID fabricmmo$owner;

    @Override
    public Optional<UUID> fabricmmo$getOwner() {
        return Optional.ofNullable(fabricmmo$owner);
    }

    @Override
    public void fabricmmo$setOwner(UUID owner) {
        fabricmmo$owner = owner;
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void fabricmmo$readOwner(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registries,
            CallbackInfo callback) {
        fabricmmo$owner = ProcessingBlockOwnershipNbt.read(nbt).orElse(null);
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void fabricmmo$writeOwner(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registries,
            CallbackInfo callback) {
        ProcessingBlockOwnershipNbt.write(nbt, fabricmmo$owner);
    }

    @Inject(method = "getFuelTime", at = @At("RETURN"), cancellable = true)
    private void fabricmmo$applyFuelEfficiency(
            ItemStack fuel,
            CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(SmeltingRuntimeHandler.fuelTime(
                (AbstractFurnaceBlockEntity) (Object) this,
                callback.getReturnValueI()));
    }

    @Inject(method = "craftRecipe", at = @At("HEAD"))
    private static void fabricmmo$captureSmelt(
            DynamicRegistryManager registries,
            RecipeEntry<?> recipe,
            DefaultedList<ItemStack> inventory,
            int maximumStackSize,
            CallbackInfoReturnable<Boolean> callback) {
        SmeltingCraftContext.begin(inventory.get(0), inventory.get(2));
    }

    @Inject(method = "craftRecipe", at = @At("RETURN"))
    private static void fabricmmo$completeSmeltCapture(
            DynamicRegistryManager registries,
            RecipeEntry<?> recipe,
            DefaultedList<ItemStack> inventory,
            int maximumStackSize,
            CallbackInfoReturnable<Boolean> callback) {
        SmeltingCraftContext.finish(callback.getReturnValue());
    }

    @Inject(method = "setLastRecipe", at = @At("HEAD"))
    private void fabricmmo$processSmelt(
            RecipeEntry<?> recipe,
            CallbackInfo callback) {
        SmeltingCraftContext.consume().ifPresent(snapshot ->
                SmeltingRuntimeHandler.processCraft(
                        (AbstractFurnaceBlockEntity) (Object) this, snapshot));
    }

    @ModifyArg(
            method = "dropExperience",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"),
            index = 2)
    private static int fabricmmo$multiplyVanillaExperience(int vanillaXp) {
        return SmeltingVanillaXpContext.multiply(vanillaXp);
    }
}

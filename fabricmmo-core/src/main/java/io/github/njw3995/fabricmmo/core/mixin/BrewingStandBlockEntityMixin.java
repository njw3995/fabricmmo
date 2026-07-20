package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.smelting.OwnedProcessingBlock;
import io.github.njw3995.fabricmmo.core.skill.smelting.ProcessingBlockOwnershipNbt;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
abstract class BrewingStandBlockEntityMixin implements OwnedProcessingBlock {
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
}

package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.combat.MobHealthbarService;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class EntityMixin {
    @Unique private boolean fabricmmo$healthbarSuspendedForWrite;

    @Inject(
            method = "writeNbt(Lnet/minecraft/nbt/NbtCompound;)Lnet/minecraft/nbt/NbtCompound;",
            at = @At("HEAD"))
    private void fabricmmo$suspendHealthbarForWrite(
            NbtCompound nbt,
            CallbackInfoReturnable<NbtCompound> callback) {
        if (FabricMmoFabricRuntime.running() && (Object) this instanceof LivingEntity living) {
            fabricmmo$healthbarSuspendedForWrite =
                    MobHealthbarService.suspendForSerialization(living);
        }
    }

    @Inject(
            method = "writeNbt(Lnet/minecraft/nbt/NbtCompound;)Lnet/minecraft/nbt/NbtCompound;",
            at = @At("RETURN"))
    private void fabricmmo$resumeHealthbarAfterWrite(
            NbtCompound nbt,
            CallbackInfoReturnable<NbtCompound> callback) {
        if (fabricmmo$healthbarSuspendedForWrite
                && (Object) this instanceof LivingEntity living) {
            fabricmmo$healthbarSuspendedForWrite = false;
            MobHealthbarService.resumeAfterSerialization(living);
        }
    }

    @Inject(
            method = "remove(Lnet/minecraft/entity/Entity$RemovalReason;)V",
            at = @At("HEAD"))
    private void fabricmmo$restoreHealthbarBeforeRemoval(
            Entity.RemovalReason reason,
            CallbackInfo callback) {
        if ((Object) this instanceof LivingEntity living) {
            MobHealthbarService.removed(living);
        }
    }
}

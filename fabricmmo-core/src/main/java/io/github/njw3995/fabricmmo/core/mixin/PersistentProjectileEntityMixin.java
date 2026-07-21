package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.crossbows.CrossbowRicochet;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedCombatRuntimeHandler;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
abstract class PersistentProjectileEntityMixin {
    @Unique private boolean fabricmmo$loadedFromNbt;
    @Unique private boolean fabricmmo$captureAttempted;

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void fabricmmo$markLoadedProjectile(NbtCompound nbt, CallbackInfo callback) {
        fabricmmo$loadedFromNbt = true;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void fabricmmo$captureRangedProjectile(CallbackInfo callback) {
        RangedCombatRuntimeHandler.projectileTick(
                (PersistentProjectileEntity) (Object) this,
                fabricmmo$loadedFromNbt,
                !fabricmmo$captureAttempted);
        fabricmmo$captureAttempted = true;
    }

    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    private void fabricmmo$ricochet(BlockHitResult hit, CallbackInfo callback) {
        if (CrossbowRicochet.tryRicochet(
                (PersistentProjectileEntity) (Object) this, hit)) {
            callback.cancel();
        }
    }

}

package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.taming.TamingRuntimeHandler;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TameableEntity.class)
abstract class TameableEntityMixin {
    @Inject(method = "setOwner", at = @At("TAIL"))
    private void fabricmmo$tamedByPlayer(PlayerEntity player, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            TamingRuntimeHandler.entityTamed(serverPlayer, (TameableEntity) (Object) this);
        }
    }
}

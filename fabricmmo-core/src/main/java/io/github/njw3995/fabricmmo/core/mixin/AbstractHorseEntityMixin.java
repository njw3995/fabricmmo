package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.taming.TamingRuntimeHandler;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Awards Taming XP at the vanilla horse bonding point. */
@Mixin(AbstractHorseEntity.class)
abstract class AbstractHorseEntityMixin {
    @Inject(method = "bondWithPlayer", at = @At("RETURN"))
    private void fabricmmo$horseBonded(
            PlayerEntity player,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && player instanceof ServerPlayerEntity serverPlayer) {
            TamingRuntimeHandler.entityTamed(
                    serverPlayer, (AbstractHorseEntity) (Object) this);
        }
    }
}

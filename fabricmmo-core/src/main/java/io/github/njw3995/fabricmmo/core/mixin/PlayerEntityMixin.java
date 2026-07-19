package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningAbilityHandler;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
abstract class PlayerEntityMixin {
    @Inject(
            method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",
            at = @At("HEAD"))
    private void fabricmmo$restoreSuperBreakerToolBeforeDrop(
            ItemStack stack,
            boolean throwRandomly,
            boolean retainOwnership,
            CallbackInfoReturnable<ItemEntity> callback) {
        if ((Object) this instanceof ServerPlayerEntity serverPlayer) {
            MiningAbilityHandler.restoreDroppedTool(serverPlayer, stack);
            ExcavationAbilityHandler.restoreDroppedTool(serverPlayer, stack);
        }
    }
}

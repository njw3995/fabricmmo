package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
    @Inject(
            method = "onPlayerCollision",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerInventory;insertStack(Lnet/minecraft/item/ItemStack;)Z"),
            cancellable = true)
    private void fabricmmo$sharePartyItem(PlayerEntity player, CallbackInfo callback) {
        if (player instanceof ServerPlayerEntity serverPlayer
                && SharedServerSystems.sharePartyItem(serverPlayer, (ItemEntity) (Object) this)) {
            callback.cancel();
        }
    }
}

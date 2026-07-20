package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.smelting.SmeltingRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.smelting.SmeltingVanillaXpContext;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FurnaceOutputSlot.class)
abstract class FurnaceOutputSlotMixin {
    @Shadow
    @Final
    private PlayerEntity player;

    @Inject(method = "onCrafted(Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
    private void fabricmmo$beginVanillaXpMultiplier(
            ItemStack output,
            CallbackInfo callback) {
        Slot slot = (Slot) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer
                && slot.inventory instanceof AbstractFurnaceBlockEntity furnace) {
            SmeltingVanillaXpContext.begin(
                    SmeltingRuntimeHandler.extractionMultiplier(
                            serverPlayer, furnace, output));
        } else {
            SmeltingVanillaXpContext.begin(1);
        }
    }

    @Inject(method = "onCrafted(Lnet/minecraft/item/ItemStack;)V", at = @At("RETURN"))
    private void fabricmmo$clearVanillaXpMultiplier(
            ItemStack output,
            CallbackInfo callback) {
        SmeltingVanillaXpContext.clear();
    }
}

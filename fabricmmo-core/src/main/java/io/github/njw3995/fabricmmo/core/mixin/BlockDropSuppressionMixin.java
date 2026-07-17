package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.mining.BlastDropSuppression;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
abstract class BlockDropSuppressionMixin {
    @Inject(
            method = "dropStack(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;"
                    + "Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void fabricmmo$suppressTrackedBlastDrops(
            World world, BlockPos pos, ItemStack stack, CallbackInfo callback) {
        if (BlastDropSuppression.active()) {
            callback.cancel();
        }
    }
}

package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.block.BlockPlacementCapture;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
abstract class WorldChunkMixin {
    @Shadow
    @Final
    private World world;

    @Inject(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;"
                    + "Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
            at = @At("RETURN"))
    private void fabricmmo$capturePlacedPosition(
            BlockPos pos,
            BlockState state,
            boolean moved,
            CallbackInfoReturnable<BlockState> callback) {
        if (callback.getReturnValue() != null && world instanceof ServerWorld serverWorld) {
            BlockPlacementCapture.record(serverWorld, pos, state);
        }
    }
}

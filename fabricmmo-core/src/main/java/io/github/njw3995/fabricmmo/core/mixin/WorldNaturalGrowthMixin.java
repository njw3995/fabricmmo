package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.block.NaturalGrowthContext;
import io.github.njw3995.fabricmmo.core.block.NaturalGrowthTracker;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
abstract class WorldNaturalGrowthMixin {
    @Unique
    private static final ThreadLocal<Deque<BlockState>> FABRICMMO_PREVIOUS_STATES =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;"
                    + "Lnet/minecraft/block/BlockState;II)Z",
            at = @At("HEAD"))
    private void fabricmmo$capturePreviousGrowthState(
            BlockPos pos,
            BlockState state,
            int flags,
            int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> callback) {
        if (NaturalGrowthContext.active() && (Object) this instanceof ServerWorld world) {
            FABRICMMO_PREVIOUS_STATES.get().push(world.getBlockState(pos));
        }
    }

    @Inject(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;"
                    + "Lnet/minecraft/block/BlockState;II)Z",
            at = @At("RETURN"))
    private void fabricmmo$clearNaturallyGrownMarker(
            BlockPos pos,
            BlockState state,
            int flags,
            int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> callback) {
        if (!NaturalGrowthContext.active() || !((Object) this instanceof ServerWorld world)) {
            return;
        }
        Deque<BlockState> states = FABRICMMO_PREVIOUS_STATES.get();
        BlockState previous = states.pop();
        if (states.isEmpty()) {
            FABRICMMO_PREVIOUS_STATES.remove();
        }
        if (callback.getReturnValueZ()) {
            NaturalGrowthTracker.blockChanged(world, pos, previous, state);
        }
    }
}

package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlockFormationTracker;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LavaFluid.class)
abstract class LavaFluidMixin {
    @Redirect(
            method = "flow(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;"
                    + "Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;"
                    + "Lnet/minecraft/fluid/FluidState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldAccess;setBlockState("
                            + "Lnet/minecraft/util/math/BlockPos;"
                            + "Lnet/minecraft/block/BlockState;I)Z"))
    private boolean fabricmmo$trackFormedMiningBlock(
            WorldAccess world,
            BlockPos pos,
            BlockState formed,
            int flags) {
        boolean changed = world.setBlockState(pos, formed, flags);
        if (changed && world instanceof ServerWorld serverWorld) {
            MiningBlockFormationTracker.markFormedBlock(serverWorld, pos, formed);
        }
        return changed;
    }
}

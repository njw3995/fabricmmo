package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlockFormationTracker;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FluidBlock.class)
abstract class FluidBlockMixin {
    @Redirect(
            method = "receiveNeighborFluids",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState("
                            + "Lnet/minecraft/util/math/BlockPos;"
                            + "Lnet/minecraft/block/BlockState;)Z"))
    private boolean fabricmmo$trackFormedMiningBlock(
            World world,
            BlockPos pos,
            BlockState formed) {
        boolean changed = world.setBlockState(pos, formed);
        if (changed && world instanceof ServerWorld serverWorld) {
            MiningBlockFormationTracker.markFormedBlock(serverWorld, pos, formed);
        }
        return changed;
    }
}

package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.block.NaturalGrowthContext;
import net.minecraft.block.AbstractBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlock.AbstractBlockState.class)
abstract class AbstractBlockStateMixin {
    @Inject(
            method = "randomTick(Lnet/minecraft/server/world/ServerWorld;"
                    + "Lnet/minecraft/util/math/BlockPos;"
                    + "Lnet/minecraft/util/math/random/Random;)V",
            at = @At("HEAD"))
    private void fabricmmo$beginNaturalRandomTick(
            ServerWorld world,
            BlockPos pos,
            Random random,
            CallbackInfo callback) {
        NaturalGrowthContext.begin();
    }

    @Inject(
            method = "randomTick(Lnet/minecraft/server/world/ServerWorld;"
                    + "Lnet/minecraft/util/math/BlockPos;"
                    + "Lnet/minecraft/util/math/random/Random;)V",
            at = @At("RETURN"))
    private void fabricmmo$endNaturalRandomTick(
            ServerWorld world,
            BlockPos pos,
            Random random,
            CallbackInfo callback) {
        NaturalGrowthContext.end();
    }
}

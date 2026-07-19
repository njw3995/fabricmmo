package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationBlockBreakHandler;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismBlockBreakHandler;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlockBreakHandler;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingBlockBreakHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
abstract class ServerPlayerInteractionManagerMixin {
    @Shadow
    protected ServerWorld world;

    @Shadow
    @Final
    protected ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at = @At("RETURN"))
    private void fabricmmo$finishMiningBlockBreak(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> callback) {
        MiningBlockBreakHandler.finishBlockBreak(world, player, pos);
        WoodcuttingBlockBreakHandler.finishBlockBreak(world, player, pos);
        ExcavationBlockBreakHandler.finishBlockBreak(world, player, pos);
        HerbalismBlockBreakHandler.finishBlockBreak(world, player, pos, callback.getReturnValueZ());
    }
}

package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonHandler.class)
abstract class PistonHandlerMixin {
    @Unique
    private World fabricmmo$world;

    @Unique
    private BlockPos fabricmmo$pistonPos;

    @Unique
    private boolean fabricmmo$retracted;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fabricmmo$captureWorld(
            World world,
            BlockPos pos,
            Direction direction,
            boolean retracted,
            CallbackInfo callback) {
        fabricmmo$world = world;
        fabricmmo$pistonPos = pos.toImmutable();
        fabricmmo$retracted = retracted;
    }

    @Inject(method = "calculatePush()Z", at = @At("RETURN"))
    private void fabricmmo$markPistonDestinations(CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValueZ() || !(fabricmmo$world instanceof ServerWorld serverWorld)
                || !FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)
                || !FabricMmoFabricRuntime.miningSettings().pistonExploitPrevention()) {
            return;
        }
        PistonHandler handler = (PistonHandler) (Object) this;
        String worldId = serverWorld.getRegistryKey().getValue().toString();
        if (fabricmmo$retracted && fabricmmo$pistonPos != null) {
            BlockPos adjacent = fabricmmo$pistonPos.offset(handler.getMotionDirection());
            FabricMmoFabricRuntime.markPlayerPlaced(new BlockLocation(
                    worldId, adjacent.getX(), adjacent.getY(), adjacent.getZ()));
        }
        for (BlockPos source : handler.getMovedBlocks()) {
            BlockPos destination = source.offset(handler.getMotionDirection());
            FabricMmoFabricRuntime.markPlayerPlaced(new BlockLocation(
                    worldId, destination.getX(), destination.getY(), destination.getZ()));
        }
    }
}

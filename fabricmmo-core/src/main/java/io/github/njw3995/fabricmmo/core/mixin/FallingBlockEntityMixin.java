package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.block.TravelingBlockCarrier;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import net.minecraft.block.BlockState;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
abstract class FallingBlockEntityMixin implements TravelingBlockCarrier {
    @Unique
    private static final ThreadLocal<BlockLocation> fabricmmo$spawnedMarker = new ThreadLocal<>();

    @Unique
    private boolean fabricmmo$carryingIneligibleBlock;

    @Inject(method = "spawnFromBlock", at = @At("HEAD"))
    private static void fabricmmo$readFallingSourceMarker(
            World world,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<FallingBlockEntity> callback) {
        fabricmmo$spawnedMarker.remove();
        if (!(world instanceof ServerWorld serverWorld)
                || !FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)) {
            return;
        }
        String worldId = serverWorld.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        if (FabricMmoFabricRuntime.isPlayerPlaced(location)) {
            fabricmmo$spawnedMarker.set(location);
        }
    }

    @Inject(method = "spawnFromBlock", at = @At("RETURN"))
    private static void fabricmmo$transferFallingSourceMarker(
            World world,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<FallingBlockEntity> callback) {
        BlockLocation location = fabricmmo$spawnedMarker.get();
        fabricmmo$spawnedMarker.remove();
        if (location == null || callback.getReturnValue() == null) {
            return;
        }
        FabricMmoFabricRuntime.clearPlayerPlaced(location);
        ((TravelingBlockCarrier) callback.getReturnValue())
                .fabricmmo$setCarryingIneligibleBlock(true);
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState("
                            + "Lnet/minecraft/util/math/BlockPos;"
                            + "Lnet/minecraft/block/BlockState;I)Z"))
    private boolean fabricmmo$transferFallingDestinationMarker(
            World world,
            BlockPos pos,
            BlockState state,
            int flags) {
        boolean changed = world.setBlockState(pos, state, flags);
        if (changed
                && fabricmmo$carryingIneligibleBlock
                && world instanceof ServerWorld serverWorld
                && FabricMmoFabricRuntime.running()
                && !FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)) {
            String worldId = serverWorld.getRegistryKey().getValue().toString();
            FabricMmoFabricRuntime.markPlayerPlaced(new BlockLocation(
                    worldId, pos.getX(), pos.getY(), pos.getZ()));
            fabricmmo$carryingIneligibleBlock = false;
        }
        return changed;
    }

    @Override
    public boolean fabricmmo$isCarryingIneligibleBlock() {
        return fabricmmo$carryingIneligibleBlock;
    }

    @Override
    public void fabricmmo$setCarryingIneligibleBlock(boolean value) {
        fabricmmo$carryingIneligibleBlock = value;
    }
}

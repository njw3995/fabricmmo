package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.block.TravelingBlockCarrier;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import net.minecraft.block.BlockState;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.entity.mob.EndermanEntity$PlaceBlockGoal")
abstract class EndermanPlaceBlockGoalMixin {
    @Shadow
    @Final
    private EndermanEntity enderman;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState("
                            + "Lnet/minecraft/util/math/BlockPos;"
                            + "Lnet/minecraft/block/BlockState;I)Z"))
    private boolean fabricmmo$transferMarkerFromEnderman(
            World world,
            BlockPos pos,
            BlockState state,
            int flags) {
        boolean changed = world.setBlockState(pos, state, flags);
        TravelingBlockCarrier carrier = (TravelingBlockCarrier) enderman;
        if (changed
                && carrier.fabricmmo$isCarryingIneligibleBlock()
                && world instanceof ServerWorld serverWorld
                && FabricMmoFabricRuntime.running()
                && !FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)) {
            String worldId = serverWorld.getRegistryKey().getValue().toString();
            FabricMmoFabricRuntime.markPlayerPlaced(new BlockLocation(
                    worldId, pos.getX(), pos.getY(), pos.getZ()));
            carrier.fabricmmo$setCarryingIneligibleBlock(false);
        }
        return changed;
    }
}

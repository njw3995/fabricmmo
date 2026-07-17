package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.block.TravelingBlockCarrier;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.entity.mob.EndermanEntity$PickUpBlockGoal")
abstract class EndermanPickUpBlockGoalMixin {
    @Shadow
    @Final
    private EndermanEntity enderman;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;removeBlock("
                            + "Lnet/minecraft/util/math/BlockPos;Z)Z"))
    private boolean fabricmmo$transferMarkerToEnderman(
            World world,
            BlockPos pos,
            boolean move) {
        boolean tracked = false;
        BlockLocation location = null;
        if (world instanceof ServerWorld serverWorld
                && FabricMmoFabricRuntime.running()
                && !FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)) {
            String worldId = serverWorld.getRegistryKey().getValue().toString();
            location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
            tracked = FabricMmoFabricRuntime.isPlayerPlaced(location);
        }
        boolean removed = world.removeBlock(pos, move);
        if (removed && tracked && location != null) {
            FabricMmoFabricRuntime.clearPlayerPlaced(location);
            ((TravelingBlockCarrier) enderman).fabricmmo$setCarryingIneligibleBlock(true);
        }
        return removed;
    }
}

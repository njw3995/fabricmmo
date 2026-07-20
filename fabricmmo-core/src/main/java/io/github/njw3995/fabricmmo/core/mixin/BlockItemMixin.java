package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.block.BlockPlacementCapture;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.skill.repair.UtilityAnvilInteractionHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(
            method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"))
    private void fabricmmo$beginPlacementCapture(
            ItemPlacementContext context,
            CallbackInfoReturnable<ActionResult> callback) {
        if (!FabricMmoFabricRuntime.running()
                || !(context.getWorld() instanceof ServerWorld world)
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)
                || !(context.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }
        BlockPlacementCapture.begin(world);
    }

    @Inject(
            method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("RETURN"))
    private void fabricmmo$finishPlacementCapture(
            ItemPlacementContext context,
            CallbackInfoReturnable<ActionResult> callback) {
        if (!(context.getWorld() instanceof ServerWorld)
                || !(context.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }
        boolean accepted = callback.getReturnValue().isAccepted();
        BlockPlacementCapture.finish(accepted);
        if (accepted
                && FabricMmoFabricRuntime.running()
                && context.getWorld() instanceof ServerWorld world
                && context.getPlayer() instanceof ServerPlayerEntity player) {
            UtilityAnvilInteractionHandler.onBlockPlaced(player, world, context.getBlockPos());
        }
    }
}

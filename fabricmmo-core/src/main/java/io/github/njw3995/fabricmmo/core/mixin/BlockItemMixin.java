package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("RETURN"))
    private void fabricmmo$markPlayerPlacedBlock(
            ItemPlacementContext context,
            CallbackInfoReturnable<ActionResult> callback) {
        ActionResult result = callback.getReturnValue();
        if (result == null || !result.isAccepted() || context.getWorld().isClient()
                || !FabricMmoFabricRuntime.running()) {
            return;
        }
        PlayerEntity player = context.getPlayer();
        if (!(player instanceof ServerPlayerEntity)) {
            return;
        }
        BlockPos pos = context.getBlockPos();
        String worldId = context.getWorld().getRegistryKey().getValue().toString();
        FabricMmoFabricRuntime.markPlayerPlaced(
                new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ()));
    }
}

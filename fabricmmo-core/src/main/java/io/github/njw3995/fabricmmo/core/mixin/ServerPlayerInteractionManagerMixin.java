package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationBlockBreakHandler;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismBlockBreakHandler;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlockBreakHandler;
import io.github.njw3995.fabricmmo.core.skill.unarmed.UnarmedBlockHandler;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingBlockBreakHandler;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import io.github.njw3995.fabricmmo.core.skill.repair.UtilityAnvilConfirmationService;
import io.github.njw3995.fabricmmo.core.skill.repair.UtilityAnvilInventorySynchronizer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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

    @Inject(method = "continueMining", at = @At("RETURN"), cancellable = true)
    private void fabricmmo$applyBerserkBlockEffects(
            BlockState state,
            BlockPos pos,
            int failedStartMiningTime,
            CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(UnarmedBlockHandler.modifyBreakingProgress(
                player, world, state, pos, callback.getReturnValueF()));
    }

    @Inject(method = "processBlockBreakingAction", at = @At("TAIL"))
    private void fabricmmo$activateBerserkAfterBlockHit(
            BlockPos pos,
            PlayerActionC2SPacket.Action action,
            net.minecraft.util.math.Direction direction,
            int worldHeight,
            int sequence,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        if (action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            UnarmedBlockHandler.activateAfterBlockAttack(player, world, pos);
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void fabricmmo$blockPendingAnvilItemUse(
            ServerPlayerEntity interactingPlayer,
            World interactionWorld,
            ItemStack stack,
            Hand hand,
            CallbackInfoReturnable<ActionResult> callback) {
        if (UtilityAnvilConfirmationService.global()
                .blocksItemUse(interactingPlayer.getUuid(), stack)) {
            UtilityAnvilInventorySynchronizer.resync(interactingPlayer);
            callback.setReturnValue(ActionResult.FAIL);
        }
    }

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

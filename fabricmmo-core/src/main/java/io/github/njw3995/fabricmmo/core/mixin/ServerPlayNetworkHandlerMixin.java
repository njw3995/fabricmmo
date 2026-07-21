package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.unarmed.UnarmedAbilityHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Handles empty-hand interactions after vanilla finishes processing the packet. */
@Mixin(ServerPlayNetworkHandler.class)
abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(
            method = "onPlayerInteractItem",
            at = @At("RETURN"))
    private void fabricmmo$prepareBerserkFromEmptyHandAirUse(
            PlayerInteractItemC2SPacket packet,
            CallbackInfo callback) {
        prepareIfEmpty(packet.getHand());
    }

    @Inject(
            method = "onPlayerInteractBlock",
            at = @At("RETURN"))
    private void fabricmmo$prepareBerserkFromEmptyHandBlockUse(
            PlayerInteractBlockC2SPacket packet,
            CallbackInfo callback) {
        prepareIfEmpty(packet.getHand());
    }

    private void prepareIfEmpty(Hand hand) {
        if (hand != Hand.MAIN_HAND) {
            return;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            UnarmedAbilityHandler.prepareFromEmptyHandInteraction(player, hand);
        }
    }
}

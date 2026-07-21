package io.github.njw3995.fabricmmo.core.skill.taming;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public final class TamingInteractionHandler {
    private TamingInteractionHandler() {}

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !(entity instanceof LivingEntity living)) return ActionResult.PASS;
            if (serverPlayer.getStackInHand(hand).isOf(Items.BONE)
                    && (living instanceof net.minecraft.entity.Tameable
                    || living instanceof net.minecraft.entity.passive.AbstractHorseEntity)) {
                return TamingRuntimeHandler.beastLore(serverPlayer, living)
                        ? ActionResult.SUCCESS : ActionResult.PASS;
            }
            TamingRuntimeHandler.playerAttacked(serverPlayer, living);
            return ActionResult.PASS;
        });
    }
}

package io.github.njw3995.fabricmmo.core.skill.alchemy;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

/** Tracks the persistent owner as the last eligible player to use a brewing stand. */
public final class AlchemyInteractionHandler {
    private AlchemyInteractionHandler() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient
                    && player instanceof ServerPlayerEntity serverPlayer
                    && world.getBlockEntity(hit.getBlockPos()) instanceof BrewingStandBlockEntity stand) {
                AlchemyRuntimeHandler.setOwner(stand, serverPlayer);
            }
            return ActionResult.PASS;
        });
    }
}

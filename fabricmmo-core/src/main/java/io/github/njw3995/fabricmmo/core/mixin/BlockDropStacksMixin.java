package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.mining.MiningBonusDropHandler;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Block.class)
abstract class BlockDropStacksMixin {
    @Redirect(
            method = "dropStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;"
                    + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;"
                    + "Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;getDroppedStacks("
                            + "Lnet/minecraft/block/BlockState;"
                            + "Lnet/minecraft/server/world/ServerWorld;"
                            + "Lnet/minecraft/util/math/BlockPos;"
                            + "Lnet/minecraft/block/entity/BlockEntity;"
                            + "Lnet/minecraft/entity/Entity;"
                            + "Lnet/minecraft/item/ItemStack;)Ljava/util/List;"))
    private static List<ItemStack> fabricmmo$applyMiningBonusDrops(
            BlockState state,
            ServerWorld serverWorld,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity entity,
            ItemStack tool) {
        List<ItemStack> drops = Block.getDroppedStacks(
                state, serverWorld, pos, blockEntity, entity, tool);
        return MiningBonusDropHandler.apply(
                state, serverWorld, pos, blockEntity, entity, tool, drops);
    }
}

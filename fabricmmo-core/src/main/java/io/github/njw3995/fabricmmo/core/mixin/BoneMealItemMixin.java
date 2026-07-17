package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.block.NaturalGrowthContext;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
abstract class BoneMealItemMixin {
    @Inject(method = "useOnFertilizable", at = @At("HEAD"))
    private static void fabricmmo$beginFertilizableGrowth(
            ItemStack stack,
            World world,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> callback) {
        NaturalGrowthContext.begin();
    }

    @Inject(method = "useOnFertilizable", at = @At("RETURN"))
    private static void fabricmmo$endFertilizableGrowth(
            ItemStack stack,
            World world,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> callback) {
        NaturalGrowthContext.end();
    }

    @Inject(method = "useOnGround", at = @At("HEAD"))
    private static void fabricmmo$beginGroundGrowth(
            ItemStack stack,
            World world,
            BlockPos pos,
            Direction facing,
            CallbackInfoReturnable<Boolean> callback) {
        NaturalGrowthContext.begin();
    }

    @Inject(method = "useOnGround", at = @At("RETURN"))
    private static void fabricmmo$endGroundGrowth(
            ItemStack stack,
            World world,
            BlockPos pos,
            Direction facing,
            CallbackInfoReturnable<Boolean> callback) {
        NaturalGrowthContext.end();
    }
}

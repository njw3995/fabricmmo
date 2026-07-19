package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.fishing.FishingRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.fishing.FishingSettings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.ModifyArgs;

/** Native Fishing hooks for mcMMO catch, wait-time, Shake, and Ice Fishing behavior. */
@Mixin(FishingBobberEntity.class)
abstract class FishingBobberEntityMixin {
    @Shadow
    @Final
    private int waitTimeReductionTicks;

    @ModifyArgs(
            method = "tickFishingLogic",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;nextInt("
                            + "Lnet/minecraft/util/math/random/Random;II)I",
                    ordinal = 2))
    private void fabricmmo$applyMasterAnglerWaitBounds(Args args) {
        FishingSettings.WaitBounds bounds = FishingRuntimeHandler.waitBounds(
                (FishingBobberEntity) (Object) this, waitTimeReductionTicks);
        args.set(1, bounds.minimumTicks());
        args.set(2, bounds.maximumTicks());
    }

    @Redirect(
            method = "tickFishingLogic",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;"
                            + "waitTimeReductionTicks:I"))
    private int fabricmmo$replaceVanillaLureReduction(FishingBobberEntity bobber) {
        return FishingRuntimeHandler.lureReductionTicks(bobber, waitTimeReductionTicks);
    }

    @Redirect(
            method = "use(Lnet/minecraft/item/ItemStack;)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/loot/LootTable;generateLoot("
                            + "Lnet/minecraft/loot/context/LootContextParameterSet;)"
                            + "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private ObjectArrayList<ItemStack> fabricmmo$processFishingCatch(
            LootTable table,
            LootContextParameterSet parameters,
            ItemStack rod) {
        ObjectArrayList<ItemStack> generated = table.generateLoot(parameters);
        return FishingRuntimeHandler.processCatch(
                (FishingBobberEntity) (Object) this, rod, generated);
    }

    @ModifyArg(
            method = "use(Lnet/minecraft/item/ItemStack;)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ExperienceOrbEntity;<init>("
                            + "Lnet/minecraft/world/World;DDDI)V"),
            index = 4)
    private int fabricmmo$boostVanillaFishingXp(int vanillaXp) {
        return FishingRuntimeHandler.vanillaXp(
                (FishingBobberEntity) (Object) this, vanillaXp);
    }

    @Inject(method = "use(Lnet/minecraft/item/ItemStack;)I", at = @At("HEAD"))
    private void fabricmmo$processShake(
            ItemStack rod,
            CallbackInfoReturnable<Integer> callback) {
        FishingRuntimeHandler.shake((FishingBobberEntity) (Object) this);
    }

    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    private void fabricmmo$processIceFishing(
            BlockHitResult hit,
            CallbackInfo callback) {
        if (FishingRuntimeHandler.iceFishing(
                (FishingBobberEntity) (Object) this, hit.getBlockPos())) {
            callback.cancel();
        }
    }
}

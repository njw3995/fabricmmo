package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.skill.acrobatics.AcrobaticsRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastDamage;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastRegistry;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedCombatRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsRuntimeHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Shadow protected float lastDamageTaken;

    @ModifyVariable(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private float fabricmmo$modifySkillDamage(float damage, DamageSource source) {
        if (!FabricMmoFabricRuntime.running()) {
            return damage;
        }
        LivingEntity victim = (LivingEntity) (Object) this;
        // Bukkit does not fire the upstream combat handler for invulnerable hits, and mcMMO
        // also explicitly rejects vanilla's half-window repeated-damage immunity.
        if (victim.isInvulnerableTo(source)
                || (victim.timeUntilRegen > 10 && damage <= lastDamageTaken)) {
            return damage;
        }
        float defensiveModified = victim instanceof ServerPlayerEntity player
                ? AcrobaticsRuntimeHandler.modifyCombatDamage(player, source, damage)
                : damage;
        float rangedModified = RangedCombatRuntimeHandler.modifyAttackDamage(
                victim, source, defensiveModified);
        float modified = SwordsRuntimeHandler.modifyAttackDamage(
                victim, source, rangedModified);
        if (!(source.getSource() instanceof TntEntity tnt)) {
            return modified;
        }
        MiningBlastRegistry.BlastData data = MiningBlastRegistry.find(tnt.getUuid()).orElse(null);
        if (data == null) {
            return modified;
        }
        if (victim.getUuid().equals(data.ownerId())) {
            return data.demolitionsExpertise()
                    ? MiningBlastDamage.ownerDamage(
                            modified,
                            FabricMmoFabricRuntime.miningSettings()
                                    .blastDamageDecreasePercent(data.rank()))
                    : modified;
        }
        return victim instanceof PlayerEntity
                ? MiningBlastDamage.bystanderDamage(modified)
                : modified;
    }
    @Inject(
            method = "modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F",
            at = @At("RETURN"),
            cancellable = true)
    private void fabricmmo$modifyAcrobaticsFallDamage(
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Float> callback) {
        LivingEntity victim = (LivingEntity) (Object) this;
        float modified = callback.getReturnValueF();
        if (victim instanceof ServerPlayerEntity player) {
            modified = AcrobaticsRuntimeHandler.modifyFallDamage(player, source, modified);
        }
        SwordsRuntimeHandler.damageMitigated(victim, source, modified);
        callback.setReturnValue(modified);
    }

    @Inject(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("RETURN"))
    private void fabricmmo$finishSwordsDamage(
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Boolean> callback) {
        LivingEntity victim = (LivingEntity) (Object) this;
        RangedCombatRuntimeHandler.damageFinished(
                victim, source, Boolean.TRUE.equals(callback.getReturnValue()));
        SwordsRuntimeHandler.damageFinished(
                victim, source, Boolean.TRUE.equals(callback.getReturnValue()));
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void fabricmmo$dropRetrievedArrows(DamageSource source, CallbackInfo callback) {
        RangedCombatRuntimeHandler.entityDied((LivingEntity) (Object) this);
    }
}

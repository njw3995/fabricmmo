package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.combat.MobHealthbarService;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.skill.acrobatics.AcrobaticsRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.axes.AxesRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.maces.MacesRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastDamage;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastRegistry;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.unarmed.UnarmedRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.taming.TamingRuntimeHandler;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Unique
    private static final ThreadLocal<Deque<DamageFrame>> FABRICMMO_DAMAGE_FRAMES =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Shadow protected float lastDamageTaken;

    @Inject(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void fabricmmo$captureHealthBeforeDamage(
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Boolean> callback) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target instanceof ServerPlayerEntity player
                && UnarmedRuntimeHandler.deflectArrow(player, source)) {
            callback.setReturnValue(false);
            return;
        }
        FABRICMMO_DAMAGE_FRAMES.get().addLast(
                new DamageFrame(target, source, target.getHealth()));
    }

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
        float tamingDefended = TamingRuntimeHandler.modifyWolfDefense(
                victim, source, defensiveModified);
        float tamingAttack = TamingRuntimeHandler.modifyAttackDamage(
                victim, source, tamingDefended);
        float modified = SwordsRuntimeHandler.modifyAttackDamage(
                victim, source, tamingAttack);
        modified = AxesRuntimeHandler.modifyAttackDamage(victim, source, modified);
        modified = UnarmedRuntimeHandler.modifyAttackDamage(victim, source, modified);
        modified = MacesRuntimeHandler.modifyAttackDamage(victim, source, modified);
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
            MobHealthbarService.prepareAttackerForFatalPlayerDamage(player, source, modified);
        }
        SwordsRuntimeHandler.damageMitigated(victim, source, modified);
        MacesRuntimeHandler.damageMitigated(victim, source, modified);
        callback.setReturnValue(modified);
    }

    @Inject(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("RETURN"))
    private void fabricmmo$finishSkillDamage(
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Boolean> callback) {
        LivingEntity target = (LivingEntity) (Object) this;
        boolean applied = Boolean.TRUE.equals(callback.getReturnValue());
        try {
            SwordsRuntimeHandler.damageFinished(target, source, applied);
            AxesRuntimeHandler.damageFinished(target, source, applied);
            UnarmedRuntimeHandler.damageFinished(target, source, applied);
            MacesRuntimeHandler.damageFinished(target, source, applied);
            TamingRuntimeHandler.damageFinished(target, applied);
            DamageFrame frame = FABRICMMO_DAMAGE_FRAMES.get().peekLast();
            if (frame != null
                    && frame.target() == target
                    && frame.source() == source
                    && applied
                    && source.getAttacker() instanceof ServerPlayerEntity
                    && frame.healthBefore() - target.getHealth() > 0.0F) {
                MobHealthbarService.showCurrentHealth(target);
            }
        } finally {
            Deque<DamageFrame> frames = FABRICMMO_DAMAGE_FRAMES.get();
            if (!frames.isEmpty()) {
                frames.removeLast();
            }
            if (frames.isEmpty()) {
                FABRICMMO_DAMAGE_FRAMES.remove();
            }
        }
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void fabricmmo$tickMobHealthbar(CallbackInfo callback) {
        if (FabricMmoFabricRuntime.running()) {
            MobHealthbarService.tick((LivingEntity) (Object) this);
        }
    }

    @Unique
    private record DamageFrame(LivingEntity target, DamageSource source, float healthBefore) {
    }
}

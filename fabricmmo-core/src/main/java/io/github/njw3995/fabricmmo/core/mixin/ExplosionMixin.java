package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.mining.BlastDropSuppression;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastProcessor;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastRegistry;
import net.minecraft.entity.TntEntity;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
abstract class ExplosionMixin {
    @Unique
    private boolean fabricmmo$trackedBlast;

    @Inject(method = "affectWorld(Z)V", at = @At("HEAD"))
    private void fabricmmo$processBlastMining(boolean particles, CallbackInfo callback) {
        Explosion explosion = (Explosion) (Object) this;
        fabricmmo$trackedBlast = MiningBlastProcessor.process(explosion);
        if (fabricmmo$trackedBlast) {
            BlastDropSuppression.begin();
        }
    }

    @Inject(method = "affectWorld(Z)V", at = @At("RETURN"))
    private void fabricmmo$finishBlastMining(boolean particles, CallbackInfo callback) {
        if (!fabricmmo$trackedBlast) {
            return;
        }
        try {
            Explosion explosion = (Explosion) (Object) this;
            if (explosion.getEntity() instanceof TntEntity tnt) {
                MiningBlastRegistry.remove(tnt.getUuid()).ifPresent(data ->
                        io.github.njw3995.fabricmmo.core.skill.mining.MiningAbilityEvents.expired(
                                data.ownerId(),
                                io.github.njw3995.fabricmmo.core.skill.mining.CoreMiningAbilities.BLAST_MINING));
            }
        } finally {
            fabricmmo$trackedBlast = false;
            BlastDropSuppression.end();
        }
    }
}

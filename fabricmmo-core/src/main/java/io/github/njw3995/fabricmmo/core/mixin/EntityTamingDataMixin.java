package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.access.TamingSummonDataAccess;
import io.github.njw3995.fabricmmo.core.skill.taming.TamingSummonState;
import io.github.njw3995.fabricmmo.core.skill.taming.TamingSummonType;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class EntityTamingDataMixin implements TamingSummonDataAccess {
    @Unique private UUID fabricmmo$summonOwner;
    @Unique private String fabricmmo$summonType;
    @Unique private long fabricmmo$summonExpiresAt;

    @Override public UUID fabricmmo$summonOwner() { return fabricmmo$summonOwner; }
    @Override public String fabricmmo$summonType() { return fabricmmo$summonType; }
    @Override public long fabricmmo$summonExpiresAt() { return fabricmmo$summonExpiresAt; }

    @Override
    public void fabricmmo$setSummonData(UUID owner, String type, long expiresAt) {
        fabricmmo$summonOwner = owner;
        fabricmmo$summonType = type;
        fabricmmo$summonExpiresAt = expiresAt;
    }

    @Override
    public void fabricmmo$clearSummonData() {
        fabricmmo$summonOwner = null;
        fabricmmo$summonType = null;
        fabricmmo$summonExpiresAt = 0L;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void fabricmmo$writeTamingData(NbtCompound nbt, CallbackInfo ci) {
        if (fabricmmo$summonOwner != null && fabricmmo$summonType != null) {
            try {
                new TamingSummonState(
                        fabricmmo$summonOwner,
                        TamingSummonType.valueOf(fabricmmo$summonType),
                        fabricmmo$summonExpiresAt).write(nbt);
            } catch (IllegalArgumentException ignored) {
                fabricmmo$clearSummonData();
            }
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void fabricmmo$readTamingData(NbtCompound nbt, CallbackInfo ci) {
        TamingSummonState.read(nbt).ifPresent(state -> {
            fabricmmo$summonOwner = state.owner();
            fabricmmo$summonType = state.type().name();
            fabricmmo$summonExpiresAt = state.expiresAt();
        });
    }
}

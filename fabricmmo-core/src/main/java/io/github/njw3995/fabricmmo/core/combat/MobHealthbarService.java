package io.github.njw3995.fabricmmo.core.combat;

import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Fabric-native equivalent of mcMMO 2.3.000's transient mob custom-name healthbars. */
public final class MobHealthbarService {
    static final int POLL_INTERVAL_TICKS = 5;
    static final int STALE_POLL_LIMIT = 100;

    private static final ConcurrentHashMap<UUID, Snapshot> ACTIVE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PendingReapply> PENDING_REAPPLY =
            new ConcurrentHashMap<>();

    private MobHealthbarService() {
    }

    /** Shows or refreshes the healthbar using the entity's current post-damage health. */
    public static void showCurrentHealth(LivingEntity target) {
        if (!FabricMmoFabricRuntime.running() || !eligible(target)) {
            return;
        }
        MobHealthbarSettings settings = FabricMmoFabricRuntime.mobHealthbarSettings();
        if (!settings.enabled()
                || settings.displayType() == MobHealthbarSettings.DisplayType.DISABLED) {
            return;
        }

        long now = System.currentTimeMillis();
        ACTIVE.compute(target.getUuid(), (uuid, existing) -> {
            Snapshot snapshot = existing == null
                    ? Snapshot.first(
                            target,
                            target.getCustomName(),
                            target.isCustomNameVisible(),
                            now,
                            target.age + settings.displaySeconds() * 20)
                    : existing.refreshed(target, now);
            applyDisplay(target, settings);
            return snapshot;
        });
    }

    /** Called once per living-entity tick to expire displays and process death-message reapply. */
    public static void tick(LivingEntity entity) {
        PendingReapply pending = PENDING_REAPPLY.get(entity.getUuid());
        if (pending != null && entity.age >= pending.dueAge()) {
            PENDING_REAPPLY.remove(entity.getUuid(), pending);
            if (eligible(entity)) {
                showCurrentHealth(entity);
            }
        }

        Snapshot snapshot = ACTIVE.get(entity.getUuid());
        if (snapshot == null) {
            return;
        }
        if (snapshot.target() != entity || entity.isRemoved() || !entity.isAlive()) {
            restore(entity);
            return;
        }
        if (entity.age < snapshot.nextPollAge()) {
            return;
        }

        long displayMillis = (long) FabricMmoFabricRuntime.mobHealthbarSettings()
                .displaySeconds() * 1_000L;
        if (System.currentTimeMillis() - snapshot.lastHitMillis() >= displayMillis) {
            restore(entity);
            return;
        }

        Snapshot polled = snapshot.polled(entity.age + POLL_INTERVAL_TICKS);
        if (polled.stalePollCount() >= STALE_POLL_LIMIT) {
            restore(entity);
            return;
        }
        ACTIVE.replace(entity.getUuid(), snapshot, polled);
    }

    /** Restores a healthbar-bearing mob before it is used as the killer in a player death message. */
    public static void prepareAttackerForFatalPlayerDamage(
            ServerPlayerEntity victim,
            DamageSource source,
            float appliedDamage) {
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof LivingEntity livingAttacker)
                || attacker instanceof ServerPlayerEntity
                || !hasActiveDisplay(livingAttacker)) {
            return;
        }
        double healthDamage = Math.max(0.0D, appliedDamage - victim.getAbsorptionAmount());
        if (victim.getHealth() - healthDamage > 0.0D) {
            return;
        }
        restore(livingAttacker);
        PENDING_REAPPLY.put(
                livingAttacker.getUuid(),
                new PendingReapply(livingAttacker.age + 1));
    }

    /** Restores and forgets transient state when an entity leaves the active world. */
    public static void removed(LivingEntity entity) {
        restore(entity);
        PENDING_REAPPLY.remove(entity.getUuid());
    }

    public static boolean hasActiveDisplay(LivingEntity entity) {
        Snapshot snapshot = ACTIVE.get(entity.getUuid());
        return snapshot != null && snapshot.target() == entity;
    }

    /** Temporarily restores the original name while vanilla serializes the entity. */
    public static boolean suspendForSerialization(LivingEntity entity) {
        Snapshot snapshot = ACTIVE.get(entity.getUuid());
        if (snapshot == null || snapshot.target() != entity) {
            return false;
        }
        setName(entity, snapshot.previousCustomName(), snapshot.previousNameVisible());
        return true;
    }

    /** Reapplies a suspended display without changing its original-name snapshot or timeout. */
    public static void resumeAfterSerialization(LivingEntity entity) {
        Snapshot snapshot = ACTIVE.get(entity.getUuid());
        if (snapshot == null || snapshot.target() != entity || !eligible(entity)) {
            return;
        }
        applyDisplay(entity, FabricMmoFabricRuntime.mobHealthbarSettings());
    }

    public static void restore(LivingEntity entity) {
        Snapshot snapshot = ACTIVE.get(entity.getUuid());
        if (snapshot == null || snapshot.target() != entity
                || !ACTIVE.remove(entity.getUuid(), snapshot)) {
            return;
        }
        setName(entity, snapshot.previousCustomName(), snapshot.previousNameVisible());
    }

    public static void reset() {
        ACTIVE.values().forEach(snapshot -> {
            LivingEntity target = snapshot.target();
            if (!target.isRemoved()) {
                setName(target, snapshot.previousCustomName(), snapshot.previousNameVisible());
            }
        });
        ACTIVE.clear();
        PENDING_REAPPLY.clear();
    }

    private static boolean eligible(LivingEntity target) {
        return target.isAlive()
                && !target.isRemoved()
                && !(target instanceof ServerPlayerEntity)
                && !(target instanceof ArmorStandEntity)
                && !(target instanceof EnderDragonEntity)
                && !(target instanceof WitherEntity);
    }

    private static void applyDisplay(LivingEntity target, MobHealthbarSettings settings) {
        target.setCustomName(MobHealthbarFormatter.text(
                settings.displayType(), target.getMaxHealth(), target.getHealth()));
        target.setCustomNameVisible(true);
    }

    private static void setName(LivingEntity target, Text customName, boolean visible) {
        target.setCustomName(customName);
        target.setCustomNameVisible(visible);
    }

    private record Snapshot(
            LivingEntity target,
            Text previousCustomName,
            boolean previousNameVisible,
            long lastHitMillis,
            int nextPollAge,
            long lastObservedLastHitMillis,
            int stalePollCount) {
        private static Snapshot first(
                LivingEntity target,
                Text previousCustomName,
                boolean previousNameVisible,
                long now,
                int firstPollAge) {
            return new Snapshot(
                    target,
                    previousCustomName,
                    previousNameVisible,
                    now,
                    firstPollAge,
                    Long.MIN_VALUE,
                    0);
        }

        private Snapshot refreshed(LivingEntity refreshedTarget, long now) {
            return new Snapshot(
                    refreshedTarget,
                    previousCustomName,
                    previousNameVisible,
                    now,
                    nextPollAge,
                    lastObservedLastHitMillis,
                    stalePollCount);
        }

        private Snapshot polled(int followingPollAge) {
            boolean unchanged = lastHitMillis == lastObservedLastHitMillis;
            return new Snapshot(
                    target,
                    previousCustomName,
                    previousNameVisible,
                    lastHitMillis,
                    followingPollAge,
                    lastHitMillis,
                    unchanged ? stalePollCount + 1 : 0);
        }
    }

    private record PendingReapply(int dueAge) {
    }
}

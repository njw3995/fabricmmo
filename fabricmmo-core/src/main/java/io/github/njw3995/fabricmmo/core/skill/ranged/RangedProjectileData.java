package io.github.njw3995.fabricmmo.core.skill.ranged;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.Vec3d;

/** In-process equivalent of mcMMO's transient Bukkit projectile/entity metadata. */
public final class RangedProjectileData {
    private static final long PROJECTILE_LIFETIME_MILLIS = Duration.ofMinutes(2).toMillis();
    private static final ConcurrentHashMap<UUID, ProjectileState> PROJECTILES =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, RetrievalState> RETRIEVALS =
            new ConcurrentHashMap<>();

    private RangedProjectileData() {
    }

    public static Optional<ProjectileState> projectile(UUID projectileId) {
        ProjectileState state = PROJECTILES.get(projectileId);
        if (state != null && state.expired(System.currentTimeMillis())) {
            PROJECTILES.remove(projectileId, state);
            return Optional.empty();
        }
        return Optional.ofNullable(state);
    }

    public static ProjectileState create(
            UUID projectileId,
            RangedWeaponKind kind,
            UUID ownerId,
            String launchWorld,
            Vec3d launchPosition,
            double forceMultiplier,
            boolean infinite,
            boolean retrievalTracked) {
        ProjectileState state = new ProjectileState(
                kind,
                ownerId,
                launchWorld,
                launchPosition,
                forceMultiplier,
                infinite,
                retrievalTracked,
                0,
                System.currentTimeMillis() + PROJECTILE_LIFETIME_MILLIS);
        PROJECTILES.put(projectileId, state);
        return state;
    }

    public static void put(UUID projectileId, ProjectileState state) {
        PROJECTILES.put(projectileId, state);
    }

    public static void putRenewed(UUID projectileId, ProjectileState state) {
        PROJECTILES.put(projectileId, state.withExpiry(
                System.currentTimeMillis() + PROJECTILE_LIFETIME_MILLIS));
    }

    public static void removeProjectile(UUID projectileId) {
        PROJECTILES.remove(projectileId);
    }

    public static boolean consumeRetrieval(UUID projectileId) {
        boolean[] consumed = {false};
        PROJECTILES.computeIfPresent(projectileId, (ignored, state) -> {
            if (state.retrievalTracked() && !state.infinite()) {
                consumed[0] = true;
                return state.withRetrievalTracked(false);
            }
            return state;
        });
        return consumed[0];
    }

    public static void addRetrieval(UUID targetId, UUID attackerId) {
        RETRIEVALS.compute(targetId, (ignored, existing) -> existing == null
                ? new RetrievalState(attackerId, 1)
                : new RetrievalState(existing.attackerId(), existing.count() + 1));
    }

    public static Optional<RetrievalState> removeRetrieval(UUID targetId) {
        return Optional.ofNullable(RETRIEVALS.remove(targetId));
    }

    public static void playerDisconnected(UUID ownerId) {
        // Projectile and corpse metadata deliberately remain during logout, matching Bukkit's
        // entity metadata lifetime. The UUID is retained for attribution if the player reconnects.
    }

    public static void cleanup() {
        long now = System.currentTimeMillis();
        PROJECTILES.entrySet().removeIf(entry -> entry.getValue().expired(now));
    }

    public static void clear() {
        PROJECTILES.clear();
        RETRIEVALS.clear();
    }

    static Map<UUID, ProjectileState> projectileSnapshot() {
        return Map.copyOf(PROJECTILES);
    }

    public record ProjectileState(
            RangedWeaponKind kind,
            UUID ownerId,
            String launchWorld,
            Vec3d launchPosition,
            double forceMultiplier,
            boolean infinite,
            boolean retrievalTracked,
            int bounceCount,
            long expiresAtMillis) {
        public ProjectileState {
            if (kind == null || ownerId == null || launchWorld == null || launchPosition == null) {
                throw new IllegalArgumentException("projectile state fields must not be null");
            }
            if (!Double.isFinite(forceMultiplier) || forceMultiplier < 0.0D) {
                throw new IllegalArgumentException("forceMultiplier must be finite and non-negative");
            }
            if (bounceCount < 0) {
                throw new IllegalArgumentException("bounceCount must be non-negative");
            }
        }

        public ProjectileState withKind(RangedWeaponKind kind) {
            return new ProjectileState(
                    kind, ownerId, launchWorld, launchPosition, forceMultiplier,
                    infinite, retrievalTracked, bounceCount, expiresAtMillis);
        }

        public ProjectileState withOwner(UUID ownerId) {
            return new ProjectileState(
                    kind, ownerId, launchWorld, launchPosition, forceMultiplier,
                    infinite, retrievalTracked, bounceCount, expiresAtMillis);
        }

        public ProjectileState withRetrievalTracked(boolean tracked) {
            return new ProjectileState(
                    kind, ownerId, launchWorld, launchPosition, forceMultiplier,
                    infinite, tracked, bounceCount, expiresAtMillis);
        }

        public ProjectileState bounced() {
            return new ProjectileState(
                    kind, ownerId, launchWorld, launchPosition, forceMultiplier,
                    infinite, retrievalTracked, bounceCount + 1, expiresAtMillis);
        }

        public ProjectileState withExpiry(long expiresAtMillis) {
            return new ProjectileState(
                    kind, ownerId, launchWorld, launchPosition, forceMultiplier,
                    infinite, retrievalTracked, bounceCount, expiresAtMillis);
        }

        boolean expired(long now) {
            return expiresAtMillis <= now;
        }
    }

    public record RetrievalState(UUID attackerId, int count) {
        public RetrievalState {
            if (attackerId == null || count <= 0) {
                throw new IllegalArgumentException("invalid retrieval state");
            }
        }
    }
}

package io.github.njw3995.fabricmmo.core.skill.combat;

import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.TameableEntity;

/** Persistent Fabric equivalent of mcMMO's combat-XP mob origin flags. */
public final class CombatMobOrigin {
    static final String EGG_TAG = "fabricmmo:combat_origin/egg";
    static final String SPAWNER_TAG = "fabricmmo:combat_origin/spawner";
    static final String NETHER_PORTAL_TAG = "fabricmmo:combat_origin/nether_portal";
    static final String BRED_TAG = "fabricmmo:combat_origin/bred";

    private CombatMobOrigin() {
    }


    public static CombatXpSettings.Origin fromSpawnReason(SpawnReason spawnReason) {
        return switch (spawnReason) {
            // Bukkit SPAWNER_EGG maps to manually used spawn eggs and shares the spawner multiplier.
            case SPAWNER, SPAWN_EGG -> CombatXpSettings.Origin.SPAWNER;
            // Bukkit DISPENSE_EGG and EGG share the egg multiplier. Thrown eggs are marked separately.
            case DISPENSER -> CombatXpSettings.Origin.EGG;
            case BREEDING -> CombatXpSettings.Origin.BRED;
            default -> CombatXpSettings.Origin.NATURAL;
        };
    }

    public static void mark(Entity entity, CombatXpSettings.Origin origin) {
        String tag = switch (origin) {
            case EGG -> EGG_TAG;
            case SPAWNER -> SPAWNER_TAG;
            case NETHER_PORTAL -> NETHER_PORTAL_TAG;
            case BRED -> BRED_TAG;
            case NATURAL, PLAYER_TAMED, CALL_OF_THE_WILD -> null;
        };
        if (tag == null) {
            return;
        }
        markRecursively(entity, tag);
    }

    public static CombatXpSettings.Origin origin(Entity entity) {
        return origin(
                entity.getCommandTags(),
                entity instanceof TameableEntity tameable && tameable.isTamed());
    }

    static CombatXpSettings.Origin origin(Set<String> tags, boolean playerTamed) {
        // Preserve upstream CombatUtils precedence when more than one flag is present.
        if (tags.contains(SPAWNER_TAG)) {
            return CombatXpSettings.Origin.SPAWNER;
        }
        if (tags.contains(NETHER_PORTAL_TAG)) {
            return CombatXpSettings.Origin.NETHER_PORTAL;
        }
        if (tags.contains(EGG_TAG)) {
            return CombatXpSettings.Origin.EGG;
        }
        if (tags.contains(BRED_TAG)) {
            return CombatXpSettings.Origin.BRED;
        }
        return playerTamed
                ? CombatXpSettings.Origin.PLAYER_TAMED
                : CombatXpSettings.Origin.NATURAL;
    }

    private static void markRecursively(Entity entity, String tag) {
        entity.addCommandTag(tag);
        for (Entity passenger : entity.getPassengerList()) {
            markRecursively(passenger, tag);
        }
    }
}

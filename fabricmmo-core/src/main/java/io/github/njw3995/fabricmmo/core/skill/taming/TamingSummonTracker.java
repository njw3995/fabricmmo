package io.github.njw3995.fabricmmo.core.skill.taming;

import io.github.njw3995.fabricmmo.core.access.TamingSummonDataAccess;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Tracks transient Call of the Wild entities and resumes cleanup after an unclean restart. */
public final class TamingSummonTracker {
    private static final Map<UUID, Summon> SUMMONS = new ConcurrentHashMap<>();

    private TamingSummonTracker() {}

    public static void register(Entity entity, UUID owner, TamingSummonType type, long expiresAt) {
        ((TamingSummonDataAccess) entity).fabricmmo$setSummonData(owner, type.name(), expiresAt);
        SUMMONS.put(entity.getUuid(), new Summon(owner, type, expiresAt));
    }

    public static boolean isSummon(Entity entity) {
        return entity instanceof TamingSummonDataAccess access
                && access.fabricmmo$summonOwner() != null;
    }

    public static int count(UUID owner, TamingSummonType type) {
        return (int) SUMMONS.values().stream()
                .filter(value -> value.owner().equals(owner) && value.type() == type)
                .count();
    }

    public static void discover(Entity entity) {
        if (!(entity instanceof TamingSummonDataAccess access)) return;
        UUID owner = access.fabricmmo$summonOwner();
        String type = access.fabricmmo$summonType();
        if (owner == null || type == null || type.isBlank()) return;
        try {
            SUMMONS.putIfAbsent(entity.getUuid(),
                    new Summon(owner, TamingSummonType.valueOf(type), access.fabricmmo$summonExpiresAt()));
        } catch (IllegalArgumentException ignored) {
            access.fabricmmo$clearSummonData();
        }
    }

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (server.getTicks() % 20 == 0) {
            for (var world : server.getWorlds()) {
                for (Entity entity : world.iterateEntities()) discover(entity);
            }
        }
        SUMMONS.entrySet().removeIf(entry -> {
            Entity entity = find(server, entry.getKey());
            if (entity == null || entity.isRemoved()) return true;
            Summon summon = entry.getValue();
            if (summon.expiresAt() > 0L && now >= summon.expiresAt()) {
                ServerPlayerEntity owner = server.getPlayerManager().getPlayer(summon.owner());
                if (owner != null) {
                    owner.sendMessage(Text.literal("Call of the Wild: your "
                            + summon.type().configName() + " departs."), false);
                }
                entity.discard();
                return true;
            }
            return false;
        });
    }

    public static void removeOwner(MinecraftServer server, UUID owner) {
        SUMMONS.entrySet().removeIf(entry -> {
            if (!entry.getValue().owner().equals(owner)) return false;
            Entity entity = find(server, entry.getKey());
            if (entity != null) entity.discard();
            return true;
        });
    }

    public static void clear(MinecraftServer server) {
        for (UUID id : SUMMONS.keySet()) {
            Entity entity = find(server, id);
            if (entity != null) entity.discard();
        }
        SUMMONS.clear();
    }

    private static Entity find(MinecraftServer server, UUID id) {
        for (var world : server.getWorlds()) {
            Entity entity = world.getEntity(id);
            if (entity != null) return entity;
        }
        return null;
    }

    private record Summon(UUID owner, TamingSummonType type, long expiresAt) {}
}

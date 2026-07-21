package io.github.njw3995.fabricmmo.core.skill.taming;

import io.github.njw3995.fabricmmo.api.event.TamingEntityTamedEvent;
import io.github.njw3995.fabricmmo.api.event.TamingSummonEvent;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import io.github.njw3995.fabricmmo.core.access.TamingSummonDataAccess;
import io.github.njw3995.fabricmmo.core.party.PartyState;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.progression.PlayerProgressionContext;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import io.github.njw3995.fabricmmo.core.skill.combat.CombatMobOrigin;
import io.github.njw3995.fabricmmo.core.skill.combat.CombatXpSettings;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Server-authoritative mcMMO 2.3.000 Taming runtime. */
public final class TamingRuntimeHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final ThreadLocal<PetHit> CURRENT_HIT = new ThreadLocal<>();
    private static final Map<UUID, Long> LAST_SUMMON_SWING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PROCESSED_TAMES = new ConcurrentHashMap<>();
    private static final long PROCESSED_TAME_TTL_MILLIS = Duration.ofMinutes(5).toMillis();

    private TamingRuntimeHandler() {}

    public static void entityTamed(ServerPlayerEntity player, LivingEntity entity) {
        long now = System.currentTimeMillis();
        if (!available(player) || TamingSummonTracker.isSummon(entity)
                || PROCESSED_TAMES.putIfAbsent(entity.getUuid(), now) != null) return;
        String path = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
        double xp = FabricMmoFabricRuntime.tamingXp(entity.getType());
        if (xp <= 0.0D) return;
        TamingEntityTamedEvent event = new TamingEntityTamedEvent(
                player.getUuid(), entity.getUuid(), path, xp);
        FabricMmoFabricRuntime.requireApi().events().publish(event);
        if (event.cancelled() || event.xp() <= 0.0D) return;
        CombatMobOrigin.mark(entity, CombatXpSettings.Origin.PLAYER_TAMED);
        FabricMmoFabricRuntime.requireApi().progression().award(new XpAwardRequest(
                player.getUuid(), CoreSkills.TAMING, CoreXpSources.TAMING_ANIMAL_TAMED,
                event.xp(), PlayerProgressionContext.enrich(player,
                        Map.of("entity", path, "context", "ANIMAL_TAMING"),
                        FabricMmoFabricRuntime.progressionSettings(), CoreSkills.TAMING)));
    }

    public static float modifyAttackDamage(LivingEntity victim, DamageSource source, float damage) {
        CURRENT_HIT.remove();
        if (!(source.getAttacker() instanceof WolfEntity wolf) || damage <= 0.0F) return damage;
        ServerPlayerEntity owner = onlineOwner(wolf);
        if (owner == null || !available(owner) || !combatEnabled(owner, victim)
                || friendlyPetDamage(owner, victim)) return damage;
        TamingSettings settings = FabricMmoFabricRuntime.tamingSettings();
        int level = level(owner);
        boolean lucky = allowed(owner, PermissionNodes.TAMING_LUCKY, false);
        float modified = damage;
        if (level >= settings.sharpenedClawsUnlock()
                && allowed(owner, PermissionNodes.TAMING_SHARPENED_CLAWS, true)) {
            modified = TamingFormula.sharpenedClaws(modified, settings.sharpenedClawsBonus());
        }
        // mcMMO 2.3.000's pinned CombatUtils path does not perform the configured Gore roll.
        if (level >= settings.goreUnlock() && allowed(owner, PermissionNodes.TAMING_GORE, true)) {
            modified = TamingFormula.goreDamage(modified, settings.goreModifier());
            notify(owner, "Combat.Gore");
            if (victim instanceof ServerPlayerEntity player) notify(player, "Combat.StruckByGore");
        }
        if (level >= settings.fastFoodUnlock()
                && allowed(owner, PermissionNodes.TAMING_FAST_FOOD, true)
                && wolf.getRandom().nextDouble()
                < TamingFormula.catalyzedChance(settings.fastFoodChance(), lucky) / 100.0D) {
            wolf.heal(damage);
        }
        if (level >= settings.pummelUnlock()
                && allowed(owner, PermissionNodes.TAMING_PUMMEL, true)
                && wolf.getRandom().nextDouble()
                < TamingFormula.catalyzedChance(settings.pummelChance(), lucky) / 100.0D) {
            Vec3d direction = victim.getPos().subtract(wolf.getPos()).normalize().multiply(1.5D);
            victim.setVelocity(direction.x, Math.max(0.1D, direction.y), direction.z);
            victim.velocityModified = true;
            if (victim instanceof ServerPlayerEntity player) notify(player,
                    "Taming.SubSkill.Pummel.TargetMessage");
        }
        CURRENT_HIT.set(new PetHit(owner.getUuid(), victim.getUuid(), victim.getHealth()));
        return modified;
    }

    public static float modifyWolfDefense(LivingEntity victim, DamageSource source, float damage) {
        if (!(victim instanceof WolfEntity wolf) || damage <= 0.0F) return damage;
        ServerPlayerEntity owner = onlineOwner(wolf);
        if (owner == null || !available(owner)) return damage;
        TamingSettings settings = FabricMmoFabricRuntime.tamingSettings();
        int level = level(owner);
        if (level >= settings.environmentallyAwareUnlock()
                && allowed(owner, PermissionNodes.TAMING_ENVIRONMENTALLY_AWARE, true)
                && isEnvironmentalHazard(source) && wolf.getHealth() > damage) {
            teleportToOwner(wolf, owner);
            notify(owner, "Taming.Listener.Wolf");
        }
        if (source.isOf(DamageTypes.ON_FIRE)
                && level >= settings.thickFurUnlock()
                && allowed(owner, PermissionNodes.TAMING_THICK_FUR, true)) {
            wolf.extinguish();
        }
        if ((source.isDirect() || source.isIn(DamageTypeTags.IS_PROJECTILE))
                && source.getAttacker() != null
                && level >= settings.thickFurUnlock()
                && allowed(owner, PermissionNodes.TAMING_THICK_FUR, true)) {
            damage = TamingFormula.reducedDamage(damage, settings.thickFurModifier());
        }
        if ((source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC)
                || source.isOf(DamageTypes.WITHER))
                && level >= settings.holyHoundUnlock()
                && allowed(owner, PermissionNodes.TAMING_HOLY_HOUND, true)) {
            wolf.heal(damage);
        }
        if ((source.isIn(DamageTypeTags.IS_EXPLOSION) || source.isIn(DamageTypeTags.IS_LIGHTNING))
                && level >= settings.shockProofUnlock()
                && allowed(owner, PermissionNodes.TAMING_SHOCK_PROOF, true)) {
            damage = TamingFormula.reducedDamage(damage, settings.shockProofModifier());
        }
        return damage;
    }

    public static boolean allowDamage(LivingEntity victim, DamageSource source) {
        if (!FabricMmoFabricRuntime.running()) return true;
        if (victim instanceof WolfEntity wolf) {
            ServerPlayerEntity owner = onlineOwner(wolf);
            if (owner != null) {
                TamingSettings settings = FabricMmoFabricRuntime.tamingSettings();
                if (source.isIn(DamageTypeTags.IS_FALL)
                        && level(owner) >= settings.environmentallyAwareUnlock()
                        && allowed(owner, PermissionNodes.TAMING_ENVIRONMENTALLY_AWARE, true)) {
                    return false;
                }
                UUID attackerOwner = effectivePlayer(source.getAttacker());
                if (attackerOwner != null && protectedRelationship(owner.getUuid(), attackerOwner)) {
                    return false;
                }
            }
        }
        UUID petOwner = effectivePlayer(source.getAttacker());
        if (petOwner != null && victim instanceof ServerPlayerEntity player
                && protectedRelationship(petOwner, player.getUuid())) return false;
        return true;
    }

    public static boolean allowTarget(Entity mob, LivingEntity target) {
        UUID owner = effectivePlayer(mob);
        if (owner == null) return true;
        UUID targetOwner = target instanceof ServerPlayerEntity player
                ? player.getUuid() : effectivePlayer(target);
        return targetOwner == null || !protectedRelationship(owner, targetOwner);
    }

    public static void damageFinished(LivingEntity victim, boolean applied) {
        PetHit hit = CURRENT_HIT.get();
        CURRENT_HIT.remove();
        if (!applied || hit == null || !hit.targetId().equals(victim.getUuid())) return;
        MinecraftServer server = victim.getServer();
        if (server == null) return;
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(hit.ownerId());
        if (owner == null || !available(owner)) return;
        double actualDamage = Math.max(0.0D, hit.healthBefore() - victim.getHealth());
        if (actualDamage <= 0.0D) return;
        CombatXpSettings settings = FabricMmoFabricRuntime.combatXpSettings();
        double baseXp;
        String context;
        if (victim instanceof ServerPlayerEntity) {
            if (!settings.pvpRewards()) return;
            baseXp = settings.pvpXp();
            context = "PVP";
        } else {
            baseXp = FabricMmoFabricRuntime.combatBaseXp(victim);
            baseXp *= settings.originMultiplier(CombatMobOrigin.origin(victim));
            context = "PVE_" + CombatMobOrigin.origin(victim).name();
        }
        int xp = settings.awardXp(baseXp * 3.0D, actualDamage);
        if (xp <= 0) return;
        FabricMmoFabricRuntime.requireApi().progression().award(new XpAwardRequest(
                owner.getUuid(), CoreSkills.TAMING, CoreXpSources.TAMING_PET_COMBAT, xp,
                PlayerProgressionContext.enrich(owner,
                        Map.of("context", context, "target", victim.getType().toString()),
                        FabricMmoFabricRuntime.progressionSettings(), CoreSkills.TAMING)));
    }

    public static boolean beastLore(ServerPlayerEntity player, LivingEntity entity) {
        if (!available(player) || level(player) < FabricMmoFabricRuntime.tamingSettings().beastLoreUnlock()
                || !allowed(player, PermissionNodes.TAMING_BEAST_LORE, true)) return false;
        UUID ownerId = entity instanceof Tameable tameable ? tameable.getOwnerUuid()
                : entity instanceof AbstractHorseEntity horse ? horse.getOwnerUuid() : null;
        player.sendMessage(LegacyText.parse(SharedServerSystems.require().locale()
                .text("Combat.BeastLore")), false);
        player.sendMessage(Text.literal("Owner (" + ownerName(player.getServer(), ownerId) + ")"), false);
        player.sendMessage(Text.literal(String.format(java.util.Locale.US,
                "Health (%.1f/%.1f)", entity.getHealth(), entity.getMaxHealth())), false);
        if (entity instanceof AbstractHorseEntity horse) {
            player.sendMessage(Text.literal(String.format(java.util.Locale.US,
                    "Horse Movement Speed (%.2f blocks/s)",
                    horse.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 43.17D)), false);
            player.sendMessage(Text.literal(String.format(java.util.Locale.US,
                    "Horse Jump Strength (Max %.2f blocks)",
                    horse.getAttributeValue(EntityAttributes.GENERIC_JUMP_STRENGTH))), false);
        }
        return true;
    }

    public static boolean trySummon(ServerPlayerEntity player) {
        if (!available(player) || !player.isSneaking()
                || level(player) < FabricMmoFabricRuntime.tamingSettings().callOfTheWildUnlock()
                || !allowed(player, PermissionNodes.TAMING_CALL_OF_THE_WILD, true)) return false;
        long now = System.currentTimeMillis();
        Long prior = LAST_SUMMON_SWING.put(player.getUuid(), now);
        if (prior != null && now - prior < 150L) return false;
        TamingSettings settings = FabricMmoFabricRuntime.tamingSettings();
        for (TamingSummonType type : TamingSummonType.values()) {
            TamingSummonSettings summon = settings.summons().get(type);
            if (!player.getMainHandStack().isOf(summon.item()) || !summonAllowed(player, type)) continue;
            int existing = TamingSummonTracker.count(player.getUuid(), type);
            if (existing + summon.summonAmount() > summon.perPlayerLimit()) {
                player.sendMessage(Text.literal("Call of the Wild: limit "
                        + summon.perPlayerLimit() + " " + type.configName() + " summon(s)."), false);
                return true;
            }
            if (!player.isCreative() && player.getMainHandStack().getCount() < summon.itemAmount()) {
                player.sendMessage(Text.literal("Call of the Wild: you need "
                        + (summon.itemAmount() - player.getMainHandStack().getCount()) + " more item(s)."), false);
                return true;
            }
            int spawned = 0;
            for (int i = 0; i < summon.summonAmount(); i++) {
                Entity entity = createSummon(player, type, summon.summonLengthSeconds(), i);
                if (entity == null) continue;
                TamingSummonEvent event = new TamingSummonEvent(player.getUuid(), entity.getUuid(),
                        type.name(), Duration.ofSeconds(summon.summonLengthSeconds()));
                FabricMmoFabricRuntime.requireApi().events().publish(event);
                if (event.cancelled()) continue;
                if (player.getServerWorld().spawnEntity(entity)) {
                    TamingSummonDataAccess data = (TamingSummonDataAccess) entity;
                    TamingSummonTracker.register(entity, player.getUuid(), type,
                            data.fabricmmo$summonExpiresAt());
                    spawned++;
                }
            }
            if (spawned > 0 && !player.isCreative()) player.getMainHandStack().decrement(summon.itemAmount());
            if (spawned > 0) player.sendMessage(Text.literal("Call of the Wild: summoned "
                    + spawned + " " + type.configName() + " for "
                    + summon.summonLengthSeconds() + " seconds."), false);
            return true;
        }
        return false;
    }

    public static void playerAttacked(ServerPlayerEntity player, LivingEntity target) {
        if (!available(player)) return;
        Box box = player.getBoundingBox().expand(5.0D);
        for (WolfEntity wolf : player.getServerWorld().getEntitiesByClass(WolfEntity.class, box,
                candidate -> candidate.isTamed() && player.getUuid().equals(candidate.getOwnerUuid())
                        && !candidate.isSitting())) {
            if (target instanceof Tameable tameable && player.getUuid().equals(tameable.getOwnerUuid())) continue;
            if (allowTarget(wolf, target)) wolf.setTarget(target);
        }
    }

    public static void tick(MinecraftServer server) {
        TamingSummonTracker.tick(server);
        if (server.getTicks() % 20 != 0) return;
        long cutoff = System.currentTimeMillis() - PROCESSED_TAME_TTL_MILLIS;
        PROCESSED_TAMES.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (!(entity instanceof WolfEntity wolf) || !wolf.isTamed() || wolf.isSitting()) continue;
                ServerPlayerEntity owner = onlineOwner(wolf);
                if (owner != null && owner.getServerWorld() != wolf.getWorld()) {
                    teleportToOwner(wolf, owner);
                }
            }
        }
    }

    public static void playerDisconnected(MinecraftServer server, UUID playerId) {
        LAST_SUMMON_SWING.remove(playerId);
        TamingSummonTracker.removeOwner(server, playerId);
    }

    public static void reset(MinecraftServer server) {
        CURRENT_HIT.remove();
        LAST_SUMMON_SWING.clear();
        PROCESSED_TAMES.clear();
        TamingSummonTracker.clear(server);
    }

    public static boolean allowBreeding(AnimalEntity first, AnimalEntity second) {
        if (!FabricMmoFabricRuntime.running()
                || !FabricMmoFabricRuntime.tamingSettings().cotwBreedingPrevented()
                || (!TamingSummonTracker.isSummon(first)
                && !TamingSummonTracker.isSummon(second))) {
            return true;
        }
        first.resetLoveTicks();
        second.resetLoveTicks();
        ServerPlayerEntity breeder = first.getLovingPlayer();
        if (breeder == null) breeder = second.getLovingPlayer();
        if (breeder != null) {
            breeder.sendMessage(Text.literal(
                    "Call of the Wild summons cannot be bred."), false);
        }
        return false;
    }

    private static Entity createSummon(ServerPlayerEntity player, TamingSummonType type,
                                       int lifetimeSeconds, int index) {
        Entity entity = switch (type) {
            case WOLF -> new WolfEntity(net.minecraft.entity.EntityType.WOLF, player.getServerWorld());
            case OCELOT -> new OcelotEntity(net.minecraft.entity.EntityType.OCELOT, player.getServerWorld());
            case HORSE -> new net.minecraft.entity.passive.HorseEntity(
                    net.minecraft.entity.EntityType.HORSE, player.getServerWorld());
        };
        entity.refreshPositionAndAngles(player.getX() + (index + 1), player.getY(),
                player.getZ() + (index % 2 == 0 ? 1 : -1), player.getYaw(), 0.0F);
        long expiresAt = lifetimeSeconds <= 0 ? 0L
                : System.currentTimeMillis() + Duration.ofSeconds(lifetimeSeconds).toMillis();
        ((TamingSummonDataAccess) entity).fabricmmo$setSummonData(
                player.getUuid(), type.name(), expiresAt);
        if (entity instanceof TameableEntity tameable) {
            tameable.setOwner(player);
            tameable.setPersistent();
            if (tameable instanceof WolfEntity wolf) {
                wolf.setHealth(wolf.getMaxHealth());
            }
        } else if (entity instanceof AbstractHorseEntity horse) {
            horse.setOwnerUuid(player.getUuid());
            horse.setTame(true);
            horse.setPersistent();
            var maxHealth = horse.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (maxHealth != null) maxHealth.setBaseValue(15.0D + horse.getRandom().nextDouble() * 15.0D);
            var jump = horse.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH);
            if (jump != null) {
                TamingSettings settings = FabricMmoFabricRuntime.tamingSettings();
                jump.setBaseValue(settings.minHorseJumpStrength()
                        + horse.getRandom().nextDouble()
                        * (settings.maxHorseJumpStrength() - settings.minHorseJumpStrength()));
            }
            horse.setHealth(horse.getMaxHealth());
        }
        entity.setCustomName(Text.literal("(COTW) " + player.getName().getString()
                + "'s " + type.configName()));
        entity.setCustomNameVisible(false);
        CombatMobOrigin.mark(entity, CombatXpSettings.Origin.CALL_OF_THE_WILD);
        return entity;
    }

    private static boolean summonAllowed(ServerPlayerEntity player, TamingSummonType type) {
        String node = switch (type) {
            case WOLF -> PermissionNodes.TAMING_COTW_WOLF;
            case OCELOT -> PermissionNodes.TAMING_COTW_OCELOT;
            case HORSE -> PermissionNodes.TAMING_COTW_HORSE;
        };
        return allowed(player, node, true);
    }

    private static boolean available(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.running()
                && !FabricMmoFabricRuntime.isWorldBlacklisted(player.getServerWorld())
                && allowed(player, PermissionNodes.TAMING, true);
    }

    private static boolean combatEnabled(ServerPlayerEntity owner, LivingEntity victim) {
        TamingSettings settings = FabricMmoFabricRuntime.tamingSettings();
        return victim instanceof ServerPlayerEntity
                ? owner.getServer().isPvpEnabled() && settings.pvpEnabled()
                : settings.pveEnabled();
    }

    private static int level(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.TAMING).level();
    }

    private static ServerPlayerEntity onlineOwner(Tameable tameable) {
        UUID owner = tameable.getOwnerUuid();
        if (owner == null || !(tameable instanceof Entity entity) || entity.getServer() == null) return null;
        return entity.getServer().getPlayerManager().getPlayer(owner);
    }

    private static UUID effectivePlayer(Entity entity) {
        if (entity instanceof ServerPlayerEntity player) return player.getUuid();
        if (entity instanceof Tameable tameable) return tameable.getOwnerUuid();
        if (entity instanceof AbstractHorseEntity horse) return horse.getOwnerUuid();
        return null;
    }

    private static boolean friendlyPetDamage(ServerPlayerEntity owner, LivingEntity victim) {
        UUID target = victim instanceof ServerPlayerEntity player ? player.getUuid()
                : effectivePlayer(victim);
        return target != null && protectedRelationship(owner.getUuid(), target);
    }

    private static boolean protectedRelationship(UUID first, UUID second) {
        if (first.equals(second)) return true;
        if (!SharedServerSystems.running()) return false;
        Optional<PartyState> a = SharedServerSystems.require().parties().partyOf(first);
        Optional<PartyState> b = SharedServerSystems.require().parties().partyOf(second);
        if (a.isEmpty() || b.isEmpty()) return false;
        boolean related = a.orElseThrow().name().equalsIgnoreCase(b.orElseThrow().name())
                || a.orElseThrow().alliance().filter(name -> name.equalsIgnoreCase(b.orElseThrow().name())).isPresent()
                || b.orElseThrow().alliance().filter(name -> name.equalsIgnoreCase(a.orElseThrow().name())).isPresent();
        if (!related) return false;
        MinecraftServer server = SharedServerSystems.require().server();
        ServerPlayerEntity firstPlayer = server.getPlayerManager().getPlayer(first);
        ServerPlayerEntity secondPlayer = server.getPlayerManager().getPlayer(second);
        return firstPlayer == null || secondPlayer == null
                || !allowed(firstPlayer, PermissionNodes.PARTY_FRIENDLY_FIRE, false)
                || !allowed(secondPlayer, PermissionNodes.PARTY_FRIENDLY_FIRE, false);
    }

    private static boolean isEnvironmentalHazard(DamageSource source) {
        return source.isOf(DamageTypes.CACTUS) || source.isOf(DamageTypes.IN_FIRE)
                || source.isOf(DamageTypes.ON_FIRE) || source.isOf(DamageTypes.LAVA)
                || source.isOf(DamageTypes.HOT_FLOOR) || source.isOf(DamageTypes.CAMPFIRE);
    }

    private static void teleportToOwner(WolfEntity wolf, ServerPlayerEntity owner) {
        if ((ServerWorld) wolf.getWorld() == owner.getServerWorld()) {
            wolf.refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(),
                    wolf.getYaw(), wolf.getPitch());
        } else {
            wolf.teleport(owner.getServerWorld(), owner.getX(), owner.getY(), owner.getZ(),
                    Set.of(), wolf.getYaw(), wolf.getPitch());
        }
    }

    private static String ownerName(MinecraftServer server, UUID owner) {
        if (owner == null) return "Unknown";
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(owner);
        return player == null ? owner.toString() : player.getName().getString();
    }

    private static void notify(ServerPlayerEntity player, String key) {
        if (!SharedServerSystems.running()
                || !SharedServerSystems.require().sessions().get(player.getUuid()).notifications()) return;
        player.sendMessage(LegacyText.parse(SharedServerSystems.require().locale().text(key)), false);
    }

    private static boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), node, fallback);
    }

    private record PetHit(UUID ownerId, UUID targetId, double healthBefore) {}
}

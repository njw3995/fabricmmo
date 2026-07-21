package io.github.njw3995.fabricmmo.core.skill.ranged;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import io.github.njw3995.fabricmmo.core.party.PartyState;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.progression.PlayerProgressionContext;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import io.github.njw3995.fabricmmo.core.skill.archery.ArcherySettings;
import io.github.njw3995.fabricmmo.core.skill.archery.CoreArcheryAbilities;
import io.github.njw3995.fabricmmo.core.skill.combat.CombatMobOrigin;
import io.github.njw3995.fabricmmo.core.skill.combat.CombatXpSettings;
import io.github.njw3995.fabricmmo.core.skill.crossbows.CrossbowsSettings;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsDamage;
import io.github.njw3995.fabricmmo.core.skill.tridents.TridentsSettings;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/** Server-authoritative mcMMO 2.3.000 Archery, Crossbows, and Tridents runtime. */
public final class RangedCombatRuntimeHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final ThreadLocal<Deque<HitContext>> HIT_CONTEXTS =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ConcurrentHashMap<UUID, Long> RESPAWNS = new ConcurrentHashMap<>();
    private static final RangedRetrievalDeathCoordinator RETRIEVAL_DEATHS =
            new RangedRetrievalDeathCoordinator();
    private static final long PVP_RESPAWN_COOLDOWN_MILLIS = 5_000L;
    private static long lastCleanupTick;

    private RangedCombatRuntimeHandler() {
    }

    public static void projectileTick(
            PersistentProjectileEntity projectile,
            boolean loadedFromNbt,
            boolean firstCapture) {
        if (!FabricMmoFabricRuntime.running()
                || projectile.getWorld().isClient()
                || !(projectile.getWorld() instanceof ServerWorld world)) {
            return;
        }
        RangedProjectileData.ProjectileState existing = RangedProjectileData
                .projectile(projectile.getUuid()).orElse(null);
        if (existing != null) {
            RangedProjectileData.ProjectileState updated = existing;
            RangedWeaponKind currentKind = classify(projectile);
            if (currentKind != updated.kind()) {
                updated = updated.withKind(currentKind);
            }
            if (projectile.getOwner() instanceof ServerPlayerEntity currentOwner
                    && !currentOwner.getUuid().equals(updated.ownerId())) {
                updated = updated.withOwner(currentOwner.getUuid());
            }
            if (updated != existing) {
                RangedProjectileData.put(projectile.getUuid(), updated);
            }
            return;
        }
        // Capture launch-only metadata once. After the 120-second metadata cleanup, an active
        // projectile must not silently recreate it on a later tick.
        if (!firstCapture) {
            return;
        }
        // Bukkit metadata is transient. A projectile loaded after a process restart does not
        // silently regain launch metadata and therefore cannot regain mcMMO ranged processing.
        if (loadedFromNbt || !(projectile.getOwner() instanceof ServerPlayerEntity owner)) {
            return;
        }
        RangedWeaponKind kind = classify(projectile);
        if (!skillAllowed(owner, kind)
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return;
        }
        double force = kind == RangedWeaponKind.ARCHERY
                && projectile.getWeaponStack().getItem() instanceof BowItem
                ? Math.min(projectile.getVelocity().length()
                        / 3.0D * FabricMmoFabricRuntime.archerySettings().forceMultiplier(), 1.0D)
                : 1.0D;
        boolean infinite = hasInfinity(world, projectile.getWeaponStack());
        boolean retrieval = false;
        if (projectile instanceof ArrowEntity
                && projectile.getPierceLevel() == 0
                && !infinite) {
            ArcherySettings settings = FabricMmoFabricRuntime.archerySettings();
            int archeryLevel = level(owner, CoreSkills.ARCHERY);
            if (settings.retrievalRank(archeryLevel) > 0) {
                boolean lucky = allowed(owner, PermissionNodes.ARCHERY_LUCKY, false);
                retrieval = RangedPassiveEvents.roll(
                        owner.getUuid(),
                        CoreArcheryAbilities.ARROW_RETRIEVAL,
                        settings.retrievalChancePercent(archeryLevel, lucky),
                        owner.getRandom());
            }
        }
        String worldId = world.getRegistryKey().getValue().toString();
        if (!FabricMmoFabricRuntime.requireApi().protection().canInteract(
                owner.getUuid(), worldId,
                projectile.getBlockX(), projectile.getBlockY(), projectile.getBlockZ())) {
            return;
        }
        RangedProjectileData.create(
                projectile.getUuid(),
                kind,
                owner.getUuid(),
                worldId,
                projectile.getPos(),
                force,
                infinite,
                retrieval);
    }

    public static float modifyAttackDamage(
            LivingEntity target,
            DamageSource source,
            float incomingDamage) {
        if (!FabricMmoFabricRuntime.running()
                || incomingDamage <= 0.0F
                || source.isOf(DamageTypes.THORNS)) {
            return incomingDamage;
        }
        HitResolution hit = resolveHit(target, source, incomingDamage);
        if (hit == null) {
            return incomingDamage;
        }
        Deque<HitContext> contexts = HIT_CONTEXTS.get();
        contexts.push(new HitContext(
                hit.attacker().getUuid(),
                target.getUuid(),
                source,
                hit.kind(),
                hit.xpMultiplier(),
                target.getHealth(),
                hit.dazed(),
                hit.projectileId(),
                hit.retrievalCandidate()));
        return hit.damage();
    }

    public static void damageFinished(
            LivingEntity target,
            DamageSource source,
            boolean damageAccepted) {
        Deque<HitContext> contexts = HIT_CONTEXTS.get();
        HitContext context = contexts.peek();
        if (context == null
                || !context.targetId().equals(target.getUuid())
                || context.source() != source) {
            return;
        }
        try {
            if (!damageAccepted || !FabricMmoFabricRuntime.running()) {
                return;
            }
            ServerPlayerEntity attacker = target.getServer() == null
                    ? null
                    : target.getServer().getPlayerManager().getPlayer(context.attackerId());
            if (attacker == null) {
                return;
            }
            double actualHealthDamage = Math.max(0.0D, context.healthBefore() - target.getHealth());
            if (actualHealthDamage <= 0.0D) {
                return;
            }
            if (context.retrievalCandidate()
                    && context.projectileId() != null
                    && RangedProjectileData.consumeRetrieval(context.projectileId())) {
                RangedProjectileData.addRetrieval(target.getUuid(), attacker.getUuid());
            }
            if (context.dazed()
                    && target instanceof ServerPlayerEntity defender
                    && defender.isAlive()) {
                applyDaze(attacker, defender);
            }
            awardCombatXp(
                    attacker,
                    target,
                    context.kind(),
                    actualHealthDamage * context.xpMultiplier());
        } finally {
            contexts.pop();
            if (contexts.isEmpty()) {
                HIT_CONTEXTS.remove();
            }
            if (RETRIEVAL_DEATHS.afterDamage(target.getUuid())) {
                dropRetrievedArrows(target);
            }
        }
    }

    public static void entityDied(LivingEntity entity) {
        HitContext active = HIT_CONTEXTS.get().peek();
        if (!RETRIEVAL_DEATHS.onDeath(
                entity.getUuid(), active != null && active.targetId().equals(entity.getUuid()))) {
            return;
        }
        dropRetrievedArrows(entity);
    }

    private static void dropRetrievedArrows(LivingEntity entity) {
        RangedProjectileData.RetrievalState state = RangedProjectileData
                .removeRetrieval(entity.getUuid()).orElse(null);
        if (state == null
                || !(entity.getWorld() instanceof ServerWorld world)
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return;
        }
        int remaining = state.count();
        while (remaining > 0) {
            int amount = Math.min(remaining, Items.ARROW.getMaxCount());
            entity.dropStack(new ItemStack(Items.ARROW, amount));
            remaining -= amount;
        }
    }

    public static void tick(MinecraftServer server) {
        long tick = server.getTicks();
        if (tick - lastCleanupTick >= 200L) {
            lastCleanupTick = tick;
            RangedProjectileData.cleanup();
        }
    }

    public static void playerRespawned(UUID playerId) {
        RESPAWNS.put(playerId, System.currentTimeMillis());
    }

    public static void playerDisconnected(UUID playerId) {
        RESPAWNS.remove(playerId);
        RangedProjectileData.playerDisconnected(playerId);
    }

    public static void reset() {
        RangedProjectileData.clear();
        RESPAWNS.clear();
        RETRIEVAL_DEATHS.clear();
        HIT_CONTEXTS.remove();
        lastCleanupTick = 0L;
    }

    private static HitResolution resolveHit(
            LivingEntity target,
            DamageSource source,
            float incomingDamage) {
        Entity direct = source.getSource();
        if (direct instanceof PersistentProjectileEntity projectile) {
            RangedProjectileData.ProjectileState state = RangedProjectileData
                    .projectile(projectile.getUuid()).orElse(null);
            if (state == null || target.getServer() == null) {
                return null;
            }
            ServerPlayerEntity attacker = target.getServer()
                    .getPlayerManager().getPlayer(state.ownerId());
            if (attacker == null || !available(attacker, target, state.kind())) {
                return null;
            }
            return projectileHit(attacker, target, projectile, state, incomingDamage);
        }
        if (source.getAttacker() instanceof ServerPlayerEntity attacker
                && direct == attacker
                && attacker.getMainHandStack().isOf(Items.TRIDENT)
                && available(attacker, target, RangedWeaponKind.TRIDENTS)) {
            return meleeTridentHit(attacker, target, incomingDamage);
        }
        return null;
    }

    private static HitResolution projectileHit(
            ServerPlayerEntity attacker,
            LivingEntity target,
            PersistentProjectileEntity projectile,
            RangedProjectileData.ProjectileState state,
            float incomingDamage) {
        int level = level(attacker, skillId(state.kind()));
        double boosted = incomingDamage;
        boolean dazed = false;
        switch (state.kind()) {
            case ARCHERY -> {
                ArcherySettings settings = FabricMmoFabricRuntime.archerySettings();
                if (settings.skillShotRank(level) > 0
                        && allowed(attacker, PermissionNodes.ARCHERY_SKILL_SHOT, true)) {
                    boosted = settings.skillShotDamage(incomingDamage, level);
                }
                if (target instanceof ServerPlayerEntity
                        && allowed(attacker, PermissionNodes.ARCHERY_DAZE, true)) {
                    boolean lucky = allowed(attacker, PermissionNodes.ARCHERY_LUCKY, false);
                    dazed = RangedPassiveEvents.roll(
                            attacker.getUuid(),
                            CoreArcheryAbilities.DAZE,
                            settings.dazeChancePercent(level, lucky),
                            attacker.getRandom());
                    if (dazed) {
                        boosted += settings.dazeBonusDamage();
                    }
                }
                if (settings.limitBreakRank(level) > 0
                        && allowed(attacker, PermissionNodes.ARCHERY_LIMIT_BREAK, true)
                        && (target instanceof ServerPlayerEntity || settings.limitBreakPve())) {
                    boosted += RangedDamage.limitBreakDamage(
                            settings.limitBreakRank(level), armorQuality(target));
                }
                double distance = state.launchWorld().equals(
                        target.getWorld().getRegistryKey().getValue().toString())
                        ? state.launchPosition().distanceTo(target.getPos()) : 0.0D;
                return new HitResolution(
                        attacker,
                        state.kind(),
                        (float) boosted,
                        state.forceMultiplier() * settings.distanceMultiplier(distance),
                        dazed,
                        projectile.getUuid(),
                        state.retrievalTracked()
                                && !state.infinite()
                                && settings.retrievalRank(level) > 0
                                && allowed(attacker, PermissionNodes.ARCHERY, true)
                                && allowed(attacker,
                                        PermissionNodes.ARCHERY_ARROW_RETRIEVAL, true));
            }
            case CROSSBOWS -> {
                CrossbowsSettings settings = FabricMmoFabricRuntime.crossbowsSettings();
                if (settings.poweredShotRank(level) > 0
                        && allowed(attacker, PermissionNodes.CROSSBOWS_POWERED_SHOT, true)) {
                    boosted = settings.poweredShotDamage(incomingDamage, level);
                }
                if (settings.limitBreakRank(level) > 0
                        && allowed(attacker, PermissionNodes.CROSSBOWS_LIMIT_BREAK, true)
                        && (target instanceof ServerPlayerEntity || settings.limitBreakPve())) {
                    boosted += RangedDamage.limitBreakDamage(
                            settings.limitBreakRank(level), armorQuality(target));
                }
                double distance = state.launchWorld().equals(
                        target.getWorld().getRegistryKey().getValue().toString())
                        ? state.launchPosition().distanceTo(target.getPos()) : 0.0D;
                return new HitResolution(
                        attacker,
                        state.kind(),
                        (float) boosted,
                        settings.distanceMultiplier(distance),
                        false,
                        projectile.getUuid(),
                        false);
            }
            case TRIDENTS -> {
                TridentsSettings settings = FabricMmoFabricRuntime.tridentsSettings();
                if (settings.impaleRank(level) > 0
                        && allowed(attacker, PermissionNodes.TRIDENTS_IMPALE, true)) {
                    boosted += settings.impaleDamage(level);
                }
                if (settings.limitBreakRank(level) > 0
                        && allowed(attacker, PermissionNodes.TRIDENTS_LIMIT_BREAK, true)
                        && (target instanceof ServerPlayerEntity || settings.limitBreakPve())) {
                    boosted += RangedDamage.limitBreakDamage(
                            settings.limitBreakRank(level), armorQuality(target));
                }
                return new HitResolution(
                        attacker, state.kind(), (float) boosted, 1.0D, false,
                        projectile.getUuid(), false);
            }
            default -> throw new IllegalStateException("Unknown ranged skill " + state.kind());
        }
    }

    private static HitResolution meleeTridentHit(
            ServerPlayerEntity attacker,
            LivingEntity target,
            float incomingDamage) {
        TridentsSettings settings = FabricMmoFabricRuntime.tridentsSettings();
        int level = level(attacker, CoreSkills.TRIDENTS);
        double attackStrength = settings.adjustForAttackCooldown()
                ? SwordsDamage.attackStrengthScale(
                        incomingDamage,
                        attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE))
                : 1.0D;
        double boosted = incomingDamage;
        if (settings.impaleRank(level) > 0
                && allowed(attacker, PermissionNodes.TRIDENTS_IMPALE, true)) {
            boosted += settings.impaleDamage(level) * attackStrength;
        }
        if (settings.limitBreakRank(level) > 0
                && allowed(attacker, PermissionNodes.TRIDENTS_LIMIT_BREAK, true)
                && (target instanceof ServerPlayerEntity || settings.limitBreakPve())) {
            boosted += RangedDamage.limitBreakDamage(
                    settings.limitBreakRank(level), armorQuality(target)) * attackStrength;
        }
        return new HitResolution(
                attacker, RangedWeaponKind.TRIDENTS, (float) boosted, 1.0D, false,
                null, false);
    }

    private static boolean available(
            ServerPlayerEntity attacker,
            LivingEntity target,
            RangedWeaponKind kind) {
        if (!(target.getWorld() instanceof ServerWorld targetWorld)
                || target instanceof ArmorStandEntity
                || target.isSpectator()
                || FabricMmoFabricRuntime.isWorldBlacklisted(targetWorld)
                || !skillAllowed(attacker, kind)
                || !FabricMmoFabricRuntime.requireApi().protection().canDamage(
                        attacker.getUuid(),
                        target.getUuid(),
                        targetWorld.getRegistryKey().getValue().toString())) {
            return false;
        }
        boolean pvpTarget = target instanceof ServerPlayerEntity
                || (target instanceof TameableEntity tameable && tameable.isTamed());
        if (target instanceof ServerPlayerEntity playerTarget) {
            if (!attacker.getServer().isPvpEnabled()
                    || (relatedPartyOrAlliance(attacker.getUuid(), playerTarget.getUuid())
                        && !mutualFriendlyFire(attacker, playerTarget))) {
                return false;
            }
        }
        if (target instanceof TameableEntity tameable && tameable.isTamed()) {
            UUID owner = tameable.getOwnerUuid();
            if (owner != null && owner.equals(attacker.getUuid())) {
                return false;
            }
            if (owner != null && relatedPartyOrAlliance(attacker.getUuid(), owner)) {
                ServerPlayerEntity onlineOwner = attacker.getServer()
                        .getPlayerManager().getPlayer(owner);
                if (onlineOwner == null || !mutualFriendlyFire(attacker, onlineOwner)) {
                    return false;
                }
            }
        }
        return switch (kind) {
            case ARCHERY -> pvpTarget
                    ? FabricMmoFabricRuntime.archerySettings().pvpEnabled()
                    : FabricMmoFabricRuntime.archerySettings().pveEnabled();
            case CROSSBOWS -> pvpTarget
                    ? FabricMmoFabricRuntime.crossbowsSettings().pvpEnabled()
                    : FabricMmoFabricRuntime.crossbowsSettings().pveEnabled();
            case TRIDENTS -> pvpTarget
                    ? FabricMmoFabricRuntime.tridentsSettings().pvpEnabled()
                    : FabricMmoFabricRuntime.tridentsSettings().pveEnabled();
        };
    }

    private static void awardCombatXp(
            ServerPlayerEntity attacker,
            LivingEntity target,
            RangedWeaponKind kind,
            double effectiveDamage) {
        CombatXpSettings settings = FabricMmoFabricRuntime.combatXpSettings();
        double baseXp;
        String context;
        if (target instanceof ServerPlayerEntity playerTarget) {
            if (!settings.pvpRewards()
                    || sameParty(attacker.getUuid(), playerTarget.getUuid())
                    || !respawnCooldownExpired(attacker.getUuid())) {
                return;
            }
            baseXp = settings.pvpXp();
            context = "PVP";
        } else {
            if (target instanceof IronGolemEntity ironGolem && ironGolem.isPlayerCreated()) {
                return;
            }
            String path = Registries.ENTITY_TYPE.getId(target.getType()).getPath();
            baseXp = settings.pveXp(
                    path, target instanceof AnimalEntity, target instanceof HostileEntity);
            CombatXpSettings.Origin origin = CombatMobOrigin.origin(target);
            baseXp *= settings.originMultiplier(origin);
            context = "PVE_" + origin.name();
        }
        int xp = settings.awardXp(baseXp, effectiveDamage);
        if (xp <= 0) {
            return;
        }
        NamespacedId skill = skillId(kind);
        Map<String, String> awardContext = PlayerProgressionContext.enrich(
                attacker,
                Map.of("context", context, "target", target.getType().toString()),
                FabricMmoFabricRuntime.progressionSettings(),
                skill);
        FabricMmoFabricRuntime.requireApi().progression().award(new XpAwardRequest(
                attacker.getUuid(), skill, xpSource(kind), xp, awardContext));
    }

    private static void applyDaze(
            ServerPlayerEntity attacker,
            ServerPlayerEntity defender) {
        float pitch = 90.0F - defender.getRandom().nextInt(181);
        defender.networkHandler.requestTeleport(
                defender.getX(), defender.getY(), defender.getZ(), defender.getYaw(), pitch);
        defender.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 10));
        notify(defender, "Combat.TouchedFuzzy");
        notify(attacker, "Combat.TargetDazed");
    }

    private static boolean hasInfinity(ServerWorld world, ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) {
            return false;
        }
        RegistryEntry.Reference<Enchantment> infinity = world.getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(Enchantments.INFINITY)
                .orElse(null);
        return infinity != null && EnchantmentHelper.getLevel(infinity, weapon) > 0;
    }

    private static RangedWeaponKind classify(PersistentProjectileEntity projectile) {
        if (projectile instanceof TridentEntity) {
            return RangedWeaponKind.TRIDENTS;
        }
        return projectile.isShotFromCrossbow()
                ? RangedWeaponKind.CROSSBOWS
                : RangedWeaponKind.ARCHERY;
    }

    private static int level(ServerPlayerEntity player, NamespacedId skill) {
        return FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), skill).level();
    }

    private static NamespacedId skillId(RangedWeaponKind kind) {
        return switch (kind) {
            case ARCHERY -> CoreSkills.ARCHERY;
            case CROSSBOWS -> CoreSkills.CROSSBOWS;
            case TRIDENTS -> CoreSkills.TRIDENTS;
        };
    }

    private static NamespacedId xpSource(RangedWeaponKind kind) {
        return switch (kind) {
            case ARCHERY -> CoreXpSources.ARCHERY_COMBAT;
            case CROSSBOWS -> CoreXpSources.CROSSBOWS_COMBAT;
            case TRIDENTS -> CoreXpSources.TRIDENTS_COMBAT;
        };
    }

    private static boolean skillAllowed(ServerPlayerEntity player, RangedWeaponKind kind) {
        return allowed(player, switch (kind) {
            case ARCHERY -> PermissionNodes.ARCHERY;
            case CROSSBOWS -> PermissionNodes.CROSSBOWS;
            case TRIDENTS -> PermissionNodes.TRIDENTS;
        }, true);
    }

    private static int armorQuality(LivingEntity target) {
        int quality = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = target.getEquippedStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String path = Registries.ITEM.getId(stack.getItem()).getPath();
            if (path.startsWith("netherite_")) quality += 12;
            else if (path.startsWith("diamond_")) quality += 6;
            else if (path.startsWith("golden_") || path.startsWith("chainmail_")) quality += 3;
            else if (path.startsWith("iron_")) quality += 2;
            else quality += 1;
        }
        return quality;
    }

    private static boolean sameParty(UUID first, UUID second) {
        if (!SharedServerSystems.running()) {
            return false;
        }
        Optional<PartyState> firstParty = SharedServerSystems.require().parties().partyOf(first);
        Optional<PartyState> secondParty = SharedServerSystems.require().parties().partyOf(second);
        return firstParty.isPresent() && secondParty.isPresent()
                && firstParty.orElseThrow().name()
                        .equalsIgnoreCase(secondParty.orElseThrow().name());
    }

    private static boolean relatedPartyOrAlliance(UUID first, UUID second) {
        if (!SharedServerSystems.running()) {
            return false;
        }
        Optional<PartyState> firstParty = SharedServerSystems.require().parties().partyOf(first);
        Optional<PartyState> secondParty = SharedServerSystems.require().parties().partyOf(second);
        if (firstParty.isEmpty() || secondParty.isEmpty()) {
            return false;
        }
        PartyState a = firstParty.orElseThrow();
        PartyState b = secondParty.orElseThrow();
        return a.name().equalsIgnoreCase(b.name())
                || a.alliance().filter(name -> name.equalsIgnoreCase(b.name())).isPresent()
                || b.alliance().filter(name -> name.equalsIgnoreCase(a.name())).isPresent();
    }

    private static boolean mutualFriendlyFire(
            ServerPlayerEntity first,
            ServerPlayerEntity second) {
        return allowed(first, PermissionNodes.PARTY_FRIENDLY_FIRE, false)
                && allowed(second, PermissionNodes.PARTY_FRIENDLY_FIRE, false);
    }

    private static boolean respawnCooldownExpired(UUID playerId) {
        Long respawn = RESPAWNS.get(playerId);
        return respawn == null
                || System.currentTimeMillis() - respawn >= PVP_RESPAWN_COOLDOWN_MILLIS;
    }

    private static void notify(ServerPlayerEntity player, String key) {
        if (!SharedServerSystems.running()
                || !SharedServerSystems.require().sessions().get(player.getUuid()).notifications()) {
            return;
        }
        Text text = LegacyText.parse(SharedServerSystems.require().locale().text(key));
        player.sendMessage(text, false);
    }

    private static boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), node, fallback);
    }

    private record HitResolution(
            ServerPlayerEntity attacker,
            RangedWeaponKind kind,
            float damage,
            double xpMultiplier,
            boolean dazed,
            UUID projectileId,
            boolean retrievalCandidate) {
    }

    private record HitContext(
            UUID attackerId,
            UUID targetId,
            DamageSource source,
            RangedWeaponKind kind,
            double xpMultiplier,
            double healthBefore,
            boolean dazed,
            UUID projectileId,
            boolean retrievalCandidate) {
    }
}

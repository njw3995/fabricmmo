package io.github.njw3995.fabricmmo.core.skill.swords;

import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.combat.MobHealthbarService;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import io.github.njw3995.fabricmmo.core.party.PartyState;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.progression.PlayerProgressionContext;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import io.github.njw3995.fabricmmo.core.skill.combat.CombatMobOrigin;
import io.github.njw3995.fabricmmo.core.skill.combat.CombatXpSettings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/** Server-authoritative mcMMO 2.3.000 Swords combat runtime. */
public final class SwordsRuntimeHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final ConcurrentHashMap<UUID, RuptureState> RUPTURES =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> RESPAWNS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_DAMAGE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<HitContext> CURRENT_HIT = new ThreadLocal<>();
    private static final long PVP_RESPAWN_COOLDOWN_MILLIS = 5_000L;

    private SwordsRuntimeHandler() {
    }

    /** Adds Stab/Limit Break and triggers Serrated Strikes before vanilla mitigation. */
    public static float modifyAttackDamage(
            LivingEntity target,
            DamageSource source,
            float incomingDamage) {
        if (Boolean.TRUE.equals(INTERNAL_DAMAGE.get())
                || !FabricMmoFabricRuntime.running()
                || incomingDamage <= 0.0F
                || source.isOf(DamageTypes.THORNS)) {
            return incomingDamage;
        }
        CURRENT_HIT.remove();
        if (target instanceof ServerPlayerEntity defender) {
            counterAttack(defender, source, incomingDamage);
        }

        Entity root = source.getAttacker();
        Entity direct = source.getSource();
        if (!(root instanceof ServerPlayerEntity attacker)
                || direct != root
                || !available(attacker, target)
                || !attacker.getMainHandStack().isIn(ItemTags.SWORDS)) {
            return incomingDamage;
        }
        SwordsSettings settings = FabricMmoFabricRuntime.swordsSettings();
        int level = level(attacker);
        double attackStrength = settings.adjustForAttackCooldown()
                ? SwordsDamage.attackStrengthScale(
                        incomingDamage,
                        attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE))
                : 1.0D;
        double boosted = incomingDamage;

        SwordsAbilityHandler.activateOnHit(attacker);
        boolean serrated = isSerratedActive(attacker.getUuid());

        if (settings.stabRank(level) > 0
                && allowed(attacker, PermissionNodes.SWORDS_STAB, true)) {
            boosted += settings.stabDamage(level) * attackStrength;
        }
        if (settings.limitBreakRank(level) > 0
                && allowed(attacker, PermissionNodes.SWORDS_LIMIT_BREAK, true)
                && (target instanceof ServerPlayerEntity || settings.limitBreakPve())) {
            int armorQuality = target instanceof ServerPlayerEntity
                    ? armorQuality(target) : 1000;
            boosted += SwordsDamage.limitBreakDamage(
                    settings.limitBreakRank(level), armorQuality) * attackStrength;
        }
        CURRENT_HIT.set(new HitContext(
                attacker.getUuid(),
                target.getUuid(),
                source,
                attackStrength,
                target.getHealth()));
        if (serrated && allowed(attacker, PermissionNodes.SWORDS_SERRATED_STRIKES, true)) {
            serratedStrikes(attacker, target, incomingDamage, attackStrength, settings);
        }
        return (float) boosted;
    }

    public static void damageFinished(
            LivingEntity target,
            DamageSource source,
            boolean damageAccepted) {
        HitContext context = CURRENT_HIT.get();
        if (context == null
                || !context.targetId().equals(target.getUuid())
                || context.source() != source) {
            return;
        }
        try {
            if (!damageAccepted || !FabricMmoFabricRuntime.running()) {
                return;
            }
            Entity attackerEntity = source.getAttacker();
            if (!(attackerEntity instanceof ServerPlayerEntity attacker)
                    || !attacker.getUuid().equals(context.attackerId())) {
                return;
            }
            double actualHealthDamage = Math.max(
                    0.0D, context.healthBefore() - target.getHealth());
            if (actualHealthDamage <= 0.0D) {
                return;
            }
            awardCombatXp(attacker, target, actualHealthDamage);
        } finally {
            CURRENT_HIT.remove();
        }
    }

    /**
     * Applies primary-hit Rupture after vanilla armor/enchantment mitigation but before
     * absorption and health subtraction. This matches upstream's use of final event damage
     * and avoids depending on the post-damage return path for effect creation.
     */
    public static void damageMitigated(
            LivingEntity target,
            DamageSource source,
            float appliedDamage) {
        HitContext context = CURRENT_HIT.get();
        if (context == null
                || !context.targetId().equals(target.getUuid())
                || context.source() != source
                || appliedDamage < 0.0F) {
            return;
        }
        Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof ServerPlayerEntity attacker)
                || !attacker.getUuid().equals(context.attackerId())) {
            return;
        }
        double healthDamage = Math.max(
                0.0D, appliedDamage - target.getAbsorptionAmount());
        if (target.getHealth() - healthDamage <= 0.0D) {
            return;
        }
        tryApplyRupture(attacker, target, context.attackStrength());
    }

    /**
     * Advances active Ruptures once per server tick using the target reference captured when
     * the effect was applied. This avoids both logical-side entity tick ambiguity and repeated
     * UUID/world lookups.
     */
    public static void tick(MinecraftServer server) {
        if (!FabricMmoFabricRuntime.running()) {
            return;
        }
        RUPTURES.forEach((targetId, state) -> {
            LivingEntity target = state.target;
            if (target.isRemoved()
                    || !target.isAlive()
                    || !(target.getWorld() instanceof ServerWorld serverWorld)
                    || serverWorld.getServer() != server) {
                RUPTURES.remove(targetId, state);
                return;
            }
            RuptureTicker.Step step = state.ticker.tick();
            if (step == RuptureTicker.Step.EXPIRED) {
                RUPTURES.remove(targetId, state);
                return;
            }
            if (step != RuptureTicker.Step.DAMAGE
                    && step != RuptureTicker.Step.DAMAGE_AND_ANIMATE) {
                return;
            }
            if (applyPureRuptureDamageShouldCancel(target, state.tickDamage)) {
                RUPTURES.remove(targetId, state);
                return;
            }
            if (step == RuptureTicker.Step.DAMAGE_AND_ANIMATE) {
                playBleedAnimation(target);
            }
        });
    }

    public static void playerRespawned(UUID playerId) {
        RESPAWNS.put(playerId, System.currentTimeMillis());
    }

    public static void playerDisconnected(UUID playerId) {
        RESPAWNS.remove(playerId);
        RUPTURES.entrySet().removeIf(entry -> entry.getValue().attackerId.equals(playerId));
    }

    public static void reset() {
        RUPTURES.clear();
        RESPAWNS.clear();
        CURRENT_HIT.remove();
        INTERNAL_DAMAGE.remove();
    }

    private static void counterAttack(
            ServerPlayerEntity defender,
            DamageSource source,
            float damage) {
        if (Boolean.TRUE.equals(INTERNAL_DAMAGE.get())
                || damage <= 0.0F
                || !counterAvailable(defender)
                || !(source.getAttacker() instanceof LivingEntity attacker)
                || source.getSource() != source.getAttacker()
                || !defender.getMainHandStack().isIn(ItemTags.SWORDS)
                || friendlyCounterSource(defender, attacker)) {
            return;
        }
        SwordsSettings settings = FabricMmoFabricRuntime.swordsSettings();
        int level = level(defender);
        if (settings.counterRank(level) <= 0
                || !allowed(defender, PermissionNodes.SWORDS_COUNTER_ATTACK, true)) {
            return;
        }
        boolean lucky = allowed(defender, PermissionNodes.SWORDS_LUCKY, false);
        double chance = settings.counterChancePercent(level, lucky);
        if (defender.getRandom().nextDouble() >= chance / 100.0D) {
            return;
        }
        float reflected = (float) SwordsDamage.counterDamage(
                damage, settings.counterDamageModifier());
        if (reflected <= 0.0F) {
            return;
        }
        withInternalDamage(() -> attacker.damage(
                defender.getDamageSources().playerAttack(defender), reflected));
        notify(defender, "Swords.Combat.Countered");
        if (attacker instanceof ServerPlayerEntity playerAttacker) {
            notify(playerAttacker, "Swords.Combat.Counter.Hit");
        }
    }

    private static void serratedStrikes(
            ServerPlayerEntity attacker,
            LivingEntity primary,
            float rawDamage,
            double attackStrength,
            SwordsSettings settings) {
        int remaining = swordTierTargets(attacker.getMainHandStack());
        if (remaining <= 0) {
            return;
        }
        double damage = SwordsDamage.serratedAoeDamage(
                rawDamage, settings.serratedDamageModifier());
        ArrayList<LivingEntity> nearby = new ArrayList<>(attacker.getServerWorld().getEntitiesByClass(
                LivingEntity.class,
                primary.getBoundingBox().expand(2.5D),
                candidate -> candidate != primary
                        && candidate != attacker
                        && candidate.isAlive()
                        && shouldAffect(attacker, candidate)));
        for (LivingEntity target : nearby) {
            if (remaining-- <= 0) {
                break;
            }
            if (target instanceof ServerPlayerEntity playerTarget) {
                notify(playerTarget, "Swords.Combat.SS.Struck");
            }
            tryApplyRupture(attacker, target, attackStrength);
            withInternalDamage(() -> target.damage(
                    attacker.getDamageSources().playerAttack(attacker), (float) damage));
        }
    }

    private static void tryApplyRupture(
            ServerPlayerEntity attacker,
            LivingEntity target,
            double attackStrength) {
        if (!available(attacker, target)) {
            return;
        }
        SwordsSettings settings = FabricMmoFabricRuntime.swordsSettings();
        int rank = settings.ruptureRank(level(attacker));
        if (rank <= 0 || !allowed(attacker, PermissionNodes.SWORDS_RUPTURE, true)) {
            return;
        }
        RuptureState existing = RUPTURES.get(target.getUuid());
        if (existing != null) {
            existing.ticker.refresh();
            return;
        }
        boolean lucky = allowed(attacker, PermissionNodes.SWORDS_LUCKY, false);
        double chance = settings.ruptureChancePercent(rank, lucky) * attackStrength;
        if (attacker.getRandom().nextDouble() * 100.0D >= chance) {
            return;
        }
        if (target instanceof ServerPlayerEntity defender && defender.isBlocking()) {
            return;
        }
        if (target instanceof ServerPlayerEntity defender) {
            notify(defender, "Swords.Combat.Bleeding.Started");
        }
        int durationTicks = Math.min(settings.ruptureDurationSeconds(
                target instanceof ServerPlayerEntity) * 20, 200);
        double tickDamage = settings.ruptureTickDamage(
                rank, target instanceof ServerPlayerEntity);
        RUPTURES.put(target.getUuid(), new RuptureState(
                attacker.getUuid(), target, tickDamage, durationTicks));
    }

    private static boolean applyPureRuptureDamageShouldCancel(
            LivingEntity target, double damage) {
        double health = target.getHealth();
        if (health <= 0.01D) {
            return false;
        }
        double adjusted = Math.min(damage, Math.max(0.0D, health - 0.01D));
        if (adjusted <= 0.0D) {
            return true;
        }
        double damagedHealth = health - adjusted;
        if (damagedHealth > target.getMaxHealth()) {
            return true;
        }
        target.setHealth((float) damagedHealth);
        MobHealthbarService.showCurrentHealth(target);
        return false;
    }

    private static void playBleedAnimation(LivingEntity target) {
        if (!FabricMmoFabricRuntime.swordsSettings().bleedParticles()) {
            return;
        }
        ServerWorld world = (ServerWorld) target.getWorld();
        double offset = 0.3D;
        double x = target.getX();
        double y = target.getEyeY();
        double z = target.getZ();
        switch (target.getRandom().nextInt(10)) {
            case 0 -> x -= offset;
            case 1 -> x += offset;
            case 2 -> y += offset;
            case 3 -> y -= offset;
            case 4 -> z -= offset;
            case 5 -> { x += offset; z += offset; }
            case 6 -> { x -= offset; z -= offset; }
            case 7 -> { x -= offset; y -= offset; z -= offset; }
            case 8 -> { x += offset; y -= offset; z += offset; }
            case 9 -> { x -= offset; y += offset; z -= offset; }
            default -> { x += offset; y += offset; z -= offset; }
        }
        // mcMMO 2.3.000 uses Bukkit Effect.STEP_SOUND with REDSTONE_WIRE here.
        // Minecraft's BLOCK_BROKEN world event is the direct 1.21.1 equivalent: it sends
        // the redstone-wire break particles and the corresponding block-breaking sound.
        world.syncWorldEvent(
                null,
                WorldEvents.BLOCK_BROKEN,
                BlockPos.ofFloored(x, y, z),
                Block.getRawIdFromState(Blocks.REDSTONE_WIRE.getDefaultState()));
    }

    private static void awardCombatXp(
            ServerPlayerEntity attacker,
            LivingEntity target,
            double finalDamage) {
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
            baseXp = FabricMmoFabricRuntime.combatBaseXp(target);
            CombatXpSettings.Origin origin = CombatMobOrigin.origin(target);
            baseXp *= settings.originMultiplier(origin);
            context = "PVE_" + origin.name();
        }
        int xp = settings.awardXp(baseXp, finalDamage);
        if (xp <= 0) {
            return;
        }
        Map<String, String> awardContext = PlayerProgressionContext.enrich(
                attacker,
                Map.of("context", context, "target", target.getType().toString()),
                FabricMmoFabricRuntime.progressionSettings(),
                CoreSkills.SWORDS);
        FabricMmoFabricRuntime.requireApi().progression().award(new XpAwardRequest(
                attacker.getUuid(),
                CoreSkills.SWORDS,
                CoreXpSources.SWORDS_COMBAT,
                xp,
                awardContext));
    }

    private static boolean available(ServerPlayerEntity player, LivingEntity target) {
        if (!FabricMmoFabricRuntime.running()
                || target instanceof ArmorStandEntity
                || FabricMmoFabricRuntime.isWorldBlacklisted(player.getServerWorld())
                || !allowed(player, PermissionNodes.SWORDS, true)) {
            return false;
        }
        SwordsSettings settings = FabricMmoFabricRuntime.swordsSettings();
        if (target instanceof ServerPlayerEntity playerTarget) {
            if (!player.getServer().isPvpEnabled() || playerTarget.isSpectator()) {
                return false;
            }
            if (relatedPartyOrAlliance(player.getUuid(), playerTarget.getUuid())
                    && !mutualFriendlyFire(player, playerTarget)) {
                return false;
            }
        }
        boolean pvpTarget = target instanceof ServerPlayerEntity
                || (target instanceof TameableEntity tameable && tameable.isTamed());
        return pvpTarget ? settings.pvpEnabled() : settings.pveEnabled();
    }

    private static boolean counterAvailable(ServerPlayerEntity defender) {
        return FabricMmoFabricRuntime.running()
                && !FabricMmoFabricRuntime.isWorldBlacklisted(defender.getServerWorld())
                && allowed(defender, PermissionNodes.SWORDS, true)
                // Pinned upstream passes the defending player to canCombatSkillsTrigger,
                // so the PVP toggle gates Counter Attack even when a mob dealt the hit.
                && FabricMmoFabricRuntime.swordsSettings().pvpEnabled();
    }

    private static boolean shouldAffect(ServerPlayerEntity attacker, LivingEntity target) {
        if (target instanceof ArmorStandEntity || target.isSpectator()) {
            return false;
        }
        if (target instanceof ServerPlayerEntity player) {
            if (!attacker.getServer().isPvpEnabled()) {
                return false;
            }
            if (relatedPartyOrAlliance(attacker.getUuid(), player.getUuid())
                    && !mutualFriendlyFire(attacker, player)) {
                return false;
            }
        }
        if (target instanceof TameableEntity tameable && tameable.isTamed()) {
            UUID owner = tameable.getOwnerUuid();
            if (owner != null && attacker.getUuid().equals(owner)) {
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
        return true;
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
            ServerPlayerEntity first, ServerPlayerEntity second) {
        return allowed(first, PermissionNodes.PARTY_FRIENDLY_FIRE, false)
                && allowed(second, PermissionNodes.PARTY_FRIENDLY_FIRE, false);
    }

    private static boolean friendlyCounterSource(
            ServerPlayerEntity defender, LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity playerAttacker) {
            return defender == playerAttacker
                    || (relatedPartyOrAlliance(
                            defender.getUuid(), playerAttacker.getUuid())
                        && !mutualFriendlyFire(defender, playerAttacker));
        }
        if (attacker instanceof TameableEntity tameable && tameable.isTamed()) {
            UUID owner = tameable.getOwnerUuid();
            if (owner == null) {
                return false;
            }
            ServerPlayerEntity onlineOwner = defender.getServer()
                    .getPlayerManager().getPlayer(owner);
            if (owner.equals(defender.getUuid())) {
                return true;
            }
            return onlineOwner != null
                    && relatedPartyOrAlliance(defender.getUuid(), owner)
                    && !mutualFriendlyFire(defender, onlineOwner);
        }
        return false;
    }

    private static boolean respawnCooldownExpired(UUID playerId) {
        Long respawn = RESPAWNS.get(playerId);
        return respawn == null || System.currentTimeMillis() - respawn >= PVP_RESPAWN_COOLDOWN_MILLIS;
    }

    private static int level(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.SWORDS).level();
    }

    private static boolean isSerratedActive(UUID playerId) {
        try {
            return FabricMmoFabricRuntime.swordsAbilities().isActive(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Serrated Strikes state", exception);
        }
    }

    private static int swordTierTargets(ItemStack stack) {
        if (stack.isOf(Items.WOODEN_SWORD) || stack.isOf(Items.GOLDEN_SWORD)) return 1;
        if (stack.isOf(Items.STONE_SWORD)) return 2;
        if (stack.isOf(Items.IRON_SWORD)) return 3;
        if (stack.isOf(Items.DIAMOND_SWORD)) return 4;
        if (stack.isOf(Items.NETHERITE_SWORD)) return 5;
        return stack.getItem() instanceof SwordItem ? 1 : 0;
    }

    private static int armorQuality(LivingEntity target) {
        int quality = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = target.getEquippedStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String path = Registries.ITEM.getId(stack.getItem()).getPath();
            if (path.startsWith("netherite_")) quality += 12;
            else if (path.startsWith("diamond_")) quality += 6;
            else if (path.startsWith("golden_") || path.startsWith("chainmail_")) quality += 3;
            else if (path.startsWith("iron_")) quality += 2;
            else quality += 1; // Upstream MaterialMapStore defaults unknown armor to tier 1.
        }
        return quality;
    }

    private static void notify(ServerPlayerEntity player, String key) {
        if (!SharedServerSystems.running()
                || !SharedServerSystems.require().sessions().get(player.getUuid()).notifications()) {
            return;
        }
        Text text = LegacyText.parse(SharedServerSystems.require().locale().text(key));
        SwordsSettings settings = FabricMmoFabricRuntime.swordsSettings();
        if (settings.subSkillMessageActionBar()) {
            player.sendMessage(text, true);
            if (settings.subSkillMessageCopyToChat()) {
                player.sendMessage(text, false);
            }
        } else {
            player.sendMessage(text, false);
        }
    }

    private static boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), node, fallback);
    }

    private static void withInternalDamage(Runnable operation) {
        boolean prior = INTERNAL_DAMAGE.get();
        INTERNAL_DAMAGE.set(Boolean.TRUE);
        try {
            operation.run();
        } finally {
            INTERNAL_DAMAGE.set(prior);
        }
    }

    private record HitContext(
            UUID attackerId,
            UUID targetId,
            DamageSource source,
            double attackStrength,
            double healthBefore) {
    }

    private static final class RuptureState {
        private final UUID attackerId;
        private final LivingEntity target;
        private final double tickDamage;
        private final RuptureTicker ticker;

        private RuptureState(
                UUID attackerId,
                LivingEntity target,
                double tickDamage,
                int totalTickCeiling) {
            this.attackerId = attackerId;
            this.target = target;
            this.tickDamage = tickDamage;
            this.ticker = new RuptureTicker(totalTickCeiling);
        }
    }
}

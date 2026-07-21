package io.github.njw3995.fabricmmo.core.skill.axes;

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
import io.github.njw3995.fabricmmo.core.skill.combat.CombatMobOrigin;
import io.github.njw3995.fabricmmo.core.skill.combat.CombatXpSettings;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsDamage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Server-authoritative mcMMO 2.3.000 Axes combat runtime. */
public final class AxesRuntimeHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final ThreadLocal<Boolean> INTERNAL_DAMAGE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<HitContext> CURRENT_HIT = new ThreadLocal<>();

    private AxesRuntimeHandler() {
    }

    /** Applies Axes combat mechanics in the exact upstream processAxeCombat order. */
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
        Entity root = source.getAttacker();
        Entity direct = source.getSource();
        if (!(root instanceof ServerPlayerEntity attacker)
                || direct != root
                || !available(attacker, target)
                || !attacker.getMainHandStack().isIn(ItemTags.AXES)) {
            return incomingDamage;
        }

        AxesSettings settings = FabricMmoFabricRuntime.axesSettings();
        int level = level(attacker);
        double attackStrength = settings.adjustForAttackCooldown()
                ? SwordsDamage.attackStrengthScale(
                        incomingDamage,
                        attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE))
                : 1.0D;
        double boosted = incomingDamage;

        AxesAbilityHandler.activateOnHit(attacker);

        if (settings.axeMasteryRank(level) > 0
                && allowed(attacker, PermissionNodes.AXES_AXE_MASTERY, true)) {
            boosted += settings.axeMasteryDamage(level) * attackStrength;
        }

        if (hasRecognizedArmor(target)
                && settings.armorImpactRank(level) > 0
                && allowed(attacker, PermissionNodes.AXES_ARMOR_IMPACT, true)) {
            armorImpact(attacker, target, level, attackStrength, settings);
        } else if (!hasRecognizedArmor(target)
                && settings.greaterImpactRank(level) > 0
                && allowed(attacker, PermissionNodes.AXES_GREATER_IMPACT, true)
                && roll(attacker, settings.greaterImpactChance(), attackStrength, settings)) {
            greaterImpact(attacker, target, settings);
            boosted += settings.greaterImpactBonusDamage() * attackStrength;
        }

        if (isSkullSplitterActive(attacker.getUuid())
                && settings.skullSplitterRank(level) > 0
                && allowed(attacker, PermissionNodes.AXES_SKULL_SPLITTER, true)) {
            skullSplitter(attacker, target, incomingDamage, attackStrength, settings);
        }

        if (settings.criticalRank(level) > 0
                && allowed(attacker, PermissionNodes.AXES_CRITICAL_STRIKES, true)
                && roll(attacker, settings.criticalChancePercent(level, false),
                        attackStrength, settings)) {
            notify(attacker, "Axes.Combat.CriticalHit", settings);
            if (target instanceof ServerPlayerEntity defender) {
                notify(defender, "Axes.Combat.CritStruck", settings);
            }
            boosted += AxesDamage.criticalExtraDamage(
                    boosted,
                    target instanceof ServerPlayerEntity,
                    settings.criticalPvpModifier(),
                    settings.criticalPveModifier()) * attackStrength;
        }

        if (settings.limitBreakRank(level) > 0
                && allowed(attacker, PermissionNodes.AXES_LIMIT_BREAK, true)
                && (target instanceof ServerPlayerEntity || settings.limitBreakPve())) {
            boosted += AxesDamage.limitBreakDamage(
                    settings.limitBreakRank(level), armorQuality(target)) * attackStrength;
        }

        CURRENT_HIT.set(new HitContext(
                attacker.getUuid(), target.getUuid(), source, target.getHealth()));
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
            if (actualHealthDamage > 0.0D) {
                awardCombatXp(attacker, target, actualHealthDamage);
            }
        } finally {
            CURRENT_HIT.remove();
        }
    }

    public static void reset() {
        CURRENT_HIT.remove();
        INTERNAL_DAMAGE.remove();
    }

    private static void armorImpact(
            ServerPlayerEntity attacker,
            LivingEntity target,
            int level,
            double attackStrength,
            AxesSettings settings) {
        double rawDamage = settings.armorImpactRawDamage(level);
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = target.getEquippedStack(slot);
            if (armor.isEmpty()
                    || !(armor.getItem() instanceof ArmorItem)
                    || !armor.isDamageable()
                    || armor.contains(DataComponentTypes.UNBREAKABLE)
                    || armor.getMaxDamage() <= 0
                    || !roll(attacker, settings.armorImpactChance(), attackStrength, settings)) {
                continue;
            }
            int unbreaking = EnchantmentHelper.getLevel(
                    target.getWorld().getRegistryManager()
                            .get(RegistryKeys.ENCHANTMENT)
                            .entryOf(Enchantments.UNBREAKING),
                    armor);
            int durability = AxesDamage.armorImpactDurabilityDamage(
                    rawDamage, unbreaking, armor.getMaxDamage());
            armor.setDamage(Math.min(armor.getDamage() + durability, armor.getMaxDamage()));
        }
    }

    private static void greaterImpact(
            ServerPlayerEntity attacker,
            LivingEntity target,
            AxesSettings settings) {
        if (settings.greaterImpactParticles()) {
            target.getWorld().createExplosion(
                    null,
                    target.getX(),
                    target.getEyeY(),
                    target.getZ(),
                    0.0F,
                    false,
                    World.ExplosionSourceType.NONE);
        }
        Vec3d direction = attacker.getRotationVec(1.0F).normalize()
                .multiply(settings.greaterImpactKnockbackModifier());
        target.setVelocity(direction);
        target.velocityModified = true;
        notify(attacker, "Axes.Combat.GI.Proc", settings);
        if (target instanceof ServerPlayerEntity defender) {
            notify(defender, "Axes.Combat.GI.Struck", settings);
        }
    }

    private static void skullSplitter(
            ServerPlayerEntity attacker,
            LivingEntity primary,
            float rawDamage,
            double attackStrength,
            AxesSettings settings) {
        int remaining = axeTierTargets(attacker.getMainHandStack());
        if (remaining <= 0) {
            return;
        }
        double damage = AxesDamage.skullSplitterDamage(
                rawDamage, settings.skullSplitterDamageModifier(), attackStrength);
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
            if (target instanceof ServerPlayerEntity defender) {
                notify(defender, "Axes.Combat.SS.Struck", settings);
            }
            withInternalDamage(() -> target.damage(
                    attacker.getDamageSources().playerAttack(attacker), (float) damage));
        }
    }

    private static boolean roll(
            ServerPlayerEntity attacker,
            double normalChance,
            double attackStrength,
            AxesSettings settings) {
        boolean lucky = allowed(attacker, PermissionNodes.AXES_LUCKY, false);
        double chance = settings.staticChancePercent(normalChance, lucky);
        return AxesProbability.succeeds(
                attacker.getRandom().nextDouble() * 100.0D, chance, attackStrength);
    }

    private static void awardCombatXp(
            ServerPlayerEntity attacker,
            LivingEntity target,
            double finalDamage) {
        CombatXpSettings settings = FabricMmoFabricRuntime.combatXpSettings();
        double baseXp;
        String context;
        if (target instanceof ServerPlayerEntity playerTarget) {
            if (!settings.pvpRewards() || sameParty(attacker.getUuid(), playerTarget.getUuid())) {
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
                CoreSkills.AXES);
        FabricMmoFabricRuntime.requireApi().progression().award(new XpAwardRequest(
                attacker.getUuid(),
                CoreSkills.AXES,
                CoreXpSources.AXES_COMBAT,
                xp,
                awardContext));
    }

    private static boolean available(ServerPlayerEntity player, LivingEntity target) {
        if (!FabricMmoFabricRuntime.running()
                || target instanceof ArmorStandEntity
                || FabricMmoFabricRuntime.isWorldBlacklisted(player.getServerWorld())
                || !allowed(player, PermissionNodes.AXES, true)) {
            return false;
        }
        AxesSettings settings = FabricMmoFabricRuntime.axesSettings();
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

    private static boolean shouldAffect(ServerPlayerEntity attacker, LivingEntity target) {
        if (target instanceof ArmorStandEntity || target.isSpectator()) {
            return false;
        }
        String worldId = target.getWorld().getRegistryKey().getValue().toString();
        if (!FabricMmoFabricRuntime.requireApi().protection().canDamage(
                attacker.getUuid(), target.getUuid(), worldId)) {
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

    private static boolean hasRecognizedArmor(LivingEntity target) {
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = target.getEquippedStack(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem) {
                return true;
            }
        }
        return false;
    }

    private static int level(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.AXES).level();
    }

    private static boolean isSkullSplitterActive(UUID playerId) {
        try {
            return FabricMmoFabricRuntime.axesAbilities().isActive(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Skull Splitter state", exception);
        }
    }

    private static int axeTierTargets(ItemStack stack) {
        if (stack.isOf(Items.WOODEN_AXE) || stack.isOf(Items.GOLDEN_AXE)) return 1;
        if (stack.isOf(Items.STONE_AXE)) return 2;
        if (stack.isOf(Items.IRON_AXE)) return 3;
        if (stack.isOf(Items.DIAMOND_AXE)) return 4;
        if (stack.isOf(Items.NETHERITE_AXE)) return 5;
        return stack.getItem() instanceof AxeItem ? 1 : 0;
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
            ServerPlayerEntity first, ServerPlayerEntity second) {
        return allowed(first, PermissionNodes.PARTY_FRIENDLY_FIRE, false)
                && allowed(second, PermissionNodes.PARTY_FRIENDLY_FIRE, false);
    }

    private static void notify(
            ServerPlayerEntity player,
            String key,
            AxesSettings settings) {
        if (!SharedServerSystems.running()
                || !SharedServerSystems.require().sessions().get(player.getUuid()).notifications()) {
            return;
        }
        Text text = LegacyText.parse(SharedServerSystems.require().locale().text(key));
        AxesSettings.NotificationSetting notification = settings.subSkillNotification();
        player.sendMessage(text, notification.actionBar());
        if (notification.actionBar() && notification.copyToChat()) {
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
            double healthBefore) {
    }
}

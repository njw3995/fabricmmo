package io.github.njw3995.fabricmmo.core.skill.unarmed;

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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

/** Server-authoritative mcMMO 2.3.000 Unarmed combat runtime. */
public final class UnarmedRuntimeHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final ThreadLocal<HitContext> CURRENT_HIT = new ThreadLocal<>();
    private static final String DISARMED_OWNER_PREFIX = "fabricmmo.disarmed.";

    private UnarmedRuntimeHandler() {
    }

    /** Applies Unarmed mechanics in the exact upstream processUnarmedCombat order. */
    public static float modifyAttackDamage(
            LivingEntity target,
            DamageSource source,
            float incomingDamage) {
        if (!FabricMmoFabricRuntime.running()
                || incomingDamage <= 0.0F
                || source.isOf(DamageTypes.THORNS)) {
            return incomingDamage;
        }
        CURRENT_HIT.remove();
        Entity root = source.getAttacker();
        Entity direct = source.getSource();
        if (!(root instanceof ServerPlayerEntity attacker)
                || direct != root
                || !available(attacker, target)) {
            return incomingDamage;
        }
        UnarmedSettings settings = FabricMmoFabricRuntime.unarmedSettings();
        if (!isUnarmed(attacker.getMainHandStack(), settings)) {
            return incomingDamage;
        }

        int level = level(attacker);
        double attackStrength = settings.adjustForAttackCooldown()
                ? SwordsDamage.attackStrengthScale(
                        incomingDamage,
                        attacker.getAttributeValue(
                                net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE))
                : 1.0D;
        double boosted = incomingDamage;

        UnarmedAbilityHandler.activateOnHit(attacker);

        if (settings.steelArmRank(level) > 0
                && allowed(attacker, PermissionNodes.UNARMED_STEEL_ARM_STYLE, true)) {
            boosted += settings.steelArmDamage(level) * attackStrength;
        }

        if (isBerserkActive(attacker.getUuid())
                && settings.berserkRank(level) > 0
                && allowed(attacker, PermissionNodes.UNARMED_BERSERK, true)) {
            // Pinned upstream applies attack strength inside berserkDamage and again to its result.
            boosted += UnarmedDamage.berserkBonus(boosted, attackStrength) * attackStrength;
        }

        if (target instanceof ServerPlayerEntity defender
                && settings.disarmRank(level) > 0
                && !defender.getMainHandStack().isEmpty()
                && allowed(attacker, PermissionNodes.UNARMED_DISARM, true)
                && rollScaled(attacker, settings.disarmChancePercent(level, false), attackStrength)
                && !ironGripSucceeds(attacker, defender, settings)) {
            disarm(defender, settings);
        }

        if (settings.limitBreakRank(level) > 0
                && allowed(attacker, PermissionNodes.UNARMED_LIMIT_BREAK, true)
                && (target instanceof ServerPlayerEntity || settings.limitBreakPve())) {
            boosted += UnarmedDamage.limitBreakDamage(
                    settings.limitBreakRank(level), armorQuality(target)) * attackStrength;
        }

        CURRENT_HIT.set(new HitContext(
                attacker.getUuid(), target.getUuid(), source, target.getHealth()));
        return (float) boosted;
    }

    /** Cancels only Bukkit-Arrow-equivalent hits, preserving spectral arrows and tridents. */
    public static boolean deflectArrow(ServerPlayerEntity defender, DamageSource source) {
        if (!FabricMmoFabricRuntime.running()
                || !(source.getSource() instanceof ArrowEntity)
                || FabricMmoFabricRuntime.isWorldBlacklisted(defender.getServerWorld())) {
            return false;
        }
        UnarmedSettings settings = FabricMmoFabricRuntime.unarmedSettings();
        int level = level(defender);
        if (!isUnarmed(defender.getMainHandStack(), settings)
                || settings.arrowDeflectRank(level) <= 0
                || !allowed(defender, PermissionNodes.UNARMED, true)
                || !allowed(defender, PermissionNodes.UNARMED_ARROW_DEFLECT, true)) {
            return false;
        }
        boolean lucky = allowed(defender, PermissionNodes.UNARMED_LUCKY, false);
        if (!rollUnscaled(defender, settings.arrowDeflectChancePercent(level, lucky))) {
            return false;
        }
        notify(defender, "Combat.ArrowDeflect", settings);
        return true;
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
            if (!(source.getAttacker() instanceof ServerPlayerEntity attacker)
                    || !attacker.getUuid().equals(context.attackerId())) {
                return;
            }
            double actualHealthDamage = Math.max(0.0D, context.healthBefore() - target.getHealth());
            if (actualHealthDamage > 0.0D) {
                awardCombatXp(attacker, target, actualHealthDamage);
            }
        } finally {
            CURRENT_HIT.remove();
        }
    }

    public static boolean mayPickUpDisarmedItem(ItemEntity item, ServerPlayerEntity player) {
        for (String tag : item.getCommandTags()) {
            if (tag.startsWith(DISARMED_OWNER_PREFIX)) {
                return tag.equals(DISARMED_OWNER_PREFIX + player.getUuid());
            }
        }
        return true;
    }

    public static void reset() {
        CURRENT_HIT.remove();
    }

    public static boolean isUnarmed(ItemStack stack, UnarmedSettings settings) {
        if (stack.isEmpty()) {
            return true;
        }
        return settings.itemsAsUnarmed() && !isMinecraftTool(stack);
    }

    private static boolean isMinecraftTool(ItemStack stack) {
        return stack.getItem() instanceof MiningToolItem
                || stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem
                || stack.getItem() instanceof HoeItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof MaceItem
                || stack.getItem() instanceof FishingRodItem;
    }

    private static boolean ironGripSucceeds(
            ServerPlayerEntity attacker,
            ServerPlayerEntity defender,
            UnarmedSettings settings) {
        int defenderLevel = level(defender);
        if (settings.ironGripRank(defenderLevel) <= 0
                || !allowed(defender, PermissionNodes.UNARMED_IRON_GRIP, true)) {
            return false;
        }
        boolean lucky = allowed(defender, PermissionNodes.UNARMED_LUCKY, false);
        if (!rollUnscaled(defender, settings.ironGripChancePercent(defenderLevel, lucky))) {
            return false;
        }
        notify(defender, "Unarmed.Ability.IronGrip.Defender", settings);
        notify(attacker, "Unarmed.Ability.IronGrip.Attacker", settings);
        return true;
    }

    private static void disarm(ServerPlayerEntity defender, UnarmedSettings settings) {
        ItemStack held = defender.getMainHandStack();
        if (held.isEmpty()) {
            return;
        }
        ItemStack droppedStack = held.copy();
        ItemEntity dropped = new ItemEntity(
                defender.getServerWorld(),
                defender.getX(),
                defender.getY(),
                defender.getZ(),
                droppedStack);
        dropped.setPickupDelay(10);
        if (settings.disarmAntiTheft()) {
            dropped.addCommandTag(DISARMED_OWNER_PREFIX + defender.getUuid());
        }
        if (defender.getServerWorld().spawnEntity(dropped)) {
            defender.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            notify(defender, "Skills.Disarmed", settings);
        }
    }

    private static boolean rollScaled(
            ServerPlayerEntity player, double normalChance, double attackStrength) {
        boolean lucky = allowed(player, PermissionNodes.UNARMED_LUCKY, false);
        double chance = lucky ? normalChance * UnarmedProbability.LUCKY_MULTIPLIER : normalChance;
        return UnarmedProbability.succeeds(
                player.getRandom().nextDouble() * 100.0D, chance, attackStrength);
    }

    private static boolean rollUnscaled(ServerPlayerEntity player, double chance) {
        return UnarmedProbability.succeedsUnscaled(
                player.getRandom().nextDouble() * 100.0D, chance);
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
            String path = Registries.ENTITY_TYPE.getId(target.getType()).getPath();
            baseXp = settings.pveXp(
                    path, target instanceof AnimalEntity, target instanceof HostileEntity);
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
                CoreSkills.UNARMED);
        FabricMmoFabricRuntime.requireApi().progression().award(new XpAwardRequest(
                attacker.getUuid(),
                CoreSkills.UNARMED,
                CoreXpSources.UNARMED_COMBAT,
                xp,
                awardContext));
    }

    private static boolean available(ServerPlayerEntity player, LivingEntity target) {
        if (!FabricMmoFabricRuntime.running()
                || target instanceof ArmorStandEntity
                || FabricMmoFabricRuntime.isWorldBlacklisted(player.getServerWorld())
                || !allowed(player, PermissionNodes.UNARMED, true)) {
            return false;
        }
        String worldId = target.getWorld().getRegistryKey().getValue().toString();
        if (!FabricMmoFabricRuntime.requireApi().protection().canDamage(
                player.getUuid(), target.getUuid(), worldId)) {
            return false;
        }
        UnarmedSettings settings = FabricMmoFabricRuntime.unarmedSettings();
        if (target instanceof ServerPlayerEntity playerTarget) {
            if (!player.getServer().isPvpEnabled() || playerTarget.isSpectator()) {
                return false;
            }
            if (relatedPartyOrAlliance(player.getUuid(), playerTarget.getUuid())
                    && !mutualFriendlyFire(player, playerTarget)) {
                return false;
            }
        }
        if (target instanceof TameableEntity tameable && tameable.isTamed()) {
            UUID owner = tameable.getOwnerUuid();
            if (owner != null && player.getUuid().equals(owner)) {
                return false;
            }
            if (owner != null && relatedPartyOrAlliance(player.getUuid(), owner)) {
                ServerPlayerEntity onlineOwner = player.getServer()
                        .getPlayerManager().getPlayer(owner);
                if (onlineOwner == null || !mutualFriendlyFire(player, onlineOwner)) {
                    return false;
                }
            }
        }
        boolean pvpTarget = target instanceof ServerPlayerEntity
                || (target instanceof TameableEntity tameable && tameable.isTamed());
        return pvpTarget ? settings.pvpEnabled() : settings.pveEnabled();
    }

    private static int level(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.UNARMED).level();
    }

    private static boolean isBerserkActive(UUID playerId) {
        try {
            return FabricMmoFabricRuntime.unarmedAbilities().isActive(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Berserk state", exception);
        }
    }

    private static int armorQuality(LivingEntity target) {
        int quality = 0;
        for (ItemStack stack : target.getArmorItems()) {
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
        if (!SharedServerSystems.running()) return false;
        Optional<PartyState> firstParty = SharedServerSystems.require().parties().partyOf(first);
        Optional<PartyState> secondParty = SharedServerSystems.require().parties().partyOf(second);
        return firstParty.isPresent() && secondParty.isPresent()
                && firstParty.orElseThrow().name()
                        .equalsIgnoreCase(secondParty.orElseThrow().name());
    }

    private static boolean relatedPartyOrAlliance(UUID first, UUID second) {
        if (!SharedServerSystems.running()) return false;
        Optional<PartyState> firstParty = SharedServerSystems.require().parties().partyOf(first);
        Optional<PartyState> secondParty = SharedServerSystems.require().parties().partyOf(second);
        if (firstParty.isEmpty() || secondParty.isEmpty()) return false;
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
            ServerPlayerEntity player, String key, UnarmedSettings settings) {
        if (!SharedServerSystems.running()
                || !SharedServerSystems.require().sessions()
                        .get(player.getUuid()).notifications()) {
            return;
        }
        Text text = LegacyText.parse(SharedServerSystems.require().locale().text(key));
        UnarmedSettings.NotificationSetting notification = settings.subSkillNotification();
        player.sendMessage(text, notification.actionBar());
        if (notification.actionBar() && notification.copyToChat()) {
            player.sendMessage(text, false);
        }
    }

    private static boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), node, fallback);
    }

    private record HitContext(
            UUID attackerId, UUID targetId, DamageSource source, double healthBefore) {
    }
}

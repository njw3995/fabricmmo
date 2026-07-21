package io.github.njw3995.fabricmmo.core.skill.maces;

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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Server-authoritative mcMMO 2.3.000 Maces combat runtime. */
public final class MacesRuntimeHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final ThreadLocal<HitContext> CURRENT_HIT = new ThreadLocal<>();

    private MacesRuntimeHandler() {
    }

    /** Applies Limit Break then Crush, matching CombatUtils#processMacesCombat. */
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
                || !attacker.getMainHandStack().isOf(Items.MACE)
                || !available(attacker, target)) {
            return incomingDamage;
        }

        MacesSettings settings = FabricMmoFabricRuntime.macesSettings();
        int level = level(attacker);
        double attackStrength = settings.adjustForAttackCooldown()
                ? SwordsDamage.attackStrengthScale(
                        incomingDamage,
                        attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE))
                : 1.0D;
        double boosted = incomingDamage;

        if (settings.limitBreakRank(level) > 0
                && allowed(attacker, PermissionNodes.MACES_LIMIT_BREAK, true)
                && (target instanceof ServerPlayerEntity || settings.limitBreakPve())) {
            boosted += MacesDamage.limitBreakDamage(
                    settings.limitBreakRank(level), armorQuality(target)) * attackStrength;
        }

        if (settings.crushRank(level) > 0
                && allowed(attacker, PermissionNodes.MACES_CRUSH, true)) {
            boosted += settings.crushDamage(level) * attackStrength;
        }

        CURRENT_HIT.set(new HitContext(
                attacker.getUuid(), target.getUuid(), source, target.getHealth(),
                level, attackStrength));
        return (float) boosted;
    }

    /** Applies Cripple after mitigation only when the final hit is non-fatal. */
    public static void damageMitigated(
            LivingEntity target,
            DamageSource source,
            float finalDamage) {
        HitContext context = CURRENT_HIT.get();
        if (context == null
                || context.source() != source
                || !context.targetId().equals(target.getUuid())
                || finalDamage <= 0.0F
                || target.getHealth() - finalDamage <= 0.0F
                || target.hasStatusEffect(StatusEffects.SLOWNESS)) {
            return;
        }
        Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof ServerPlayerEntity attacker)
                || !attacker.getUuid().equals(context.attackerId())) {
            return;
        }
        MacesSettings settings = FabricMmoFabricRuntime.macesSettings();
        int rank = settings.crippleRank(context.level());
        if (rank <= 0 || !allowed(attacker, PermissionNodes.MACES_CRIPPLE, true)) {
            return;
        }
        boolean lucky = allowed(attacker, PermissionNodes.MACES_LUCKY, false);
        double chance = settings.crippleChancePercent(context.level(), lucky);
        if (!MacesProbability.succeeds(
                attacker.getRandom().nextDouble() * 100.0D,
                chance,
                context.attackStrength())) {
            return;
        }

        notify(attacker, "Maces.SubSkill.Cripple.Activated", settings);
        boolean playerTarget = target instanceof ServerPlayerEntity;
        target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS,
                MacesSettings.crippleDurationTicks(playerTarget),
                MacesSettings.crippleAmplifier(playerTarget)));
        playCrippleEffect(target, settings);
    }

    public static void damageFinished(
            LivingEntity target,
            DamageSource source,
            boolean damageAccepted) {
        HitContext context = CURRENT_HIT.get();
        if (context == null
                || context.source() != source
                || !context.targetId().equals(target.getUuid())) {
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
    }

    private static void playCrippleEffect(LivingEntity target, MacesSettings settings) {
        if (!settings.crippleEffectEnabled()) {
            return;
        }
        MacesSettings.SoundSetting configured = settings.crippleSound();
        Identifier id = Identifier.tryParse(configured.id());
        if (configured.enabled() && configured.volume() > 0.0D && id != null) {
            target.getWorld().playSound(
                    null,
                    target.getBlockPos(),
                    SoundEvent.of(id),
                    SoundCategory.PLAYERS,
                    (float) configured.volume(),
                    (float) Math.min(2.0D, configured.pitch() + 0.2D));
        }
        if (target instanceof ServerPlayerEntity player) {
            notify(player, "Maces.SubSkill.Cripple.Proc", settings);
        }
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
                CoreSkills.MACES);
        FabricMmoFabricRuntime.requireApi().progression().award(new XpAwardRequest(
                attacker.getUuid(),
                CoreSkills.MACES,
                CoreXpSources.MACES_COMBAT,
                xp,
                awardContext));
    }

    private static boolean available(ServerPlayerEntity player, LivingEntity target) {
        if (target instanceof ArmorStandEntity
                || FabricMmoFabricRuntime.isWorldBlacklisted(player.getServerWorld())
                || !allowed(player, PermissionNodes.MACES, true)) {
            return false;
        }
        String worldId = target.getWorld().getRegistryKey().getValue().toString();
        if (!FabricMmoFabricRuntime.requireApi().protection().canDamage(
                player.getUuid(), target.getUuid(), worldId)) {
            return false;
        }
        MacesSettings settings = FabricMmoFabricRuntime.macesSettings();
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
                ServerPlayerEntity onlineOwner = player.getServer().getPlayerManager().getPlayer(owner);
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
                .query(player.getUuid(), CoreSkills.MACES).level();
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
            MacesSettings settings) {
        if (!SharedServerSystems.running()
                || !SharedServerSystems.require().sessions().get(player.getUuid()).notifications()) {
            return;
        }
        Text text = LegacyText.parse(SharedServerSystems.require().locale().text(key));
        MacesSettings.NotificationSetting notification = settings.subSkillNotification();
        player.sendMessage(text, notification.actionBar());
        if (notification.actionBar() && notification.copyToChat()) {
            player.sendMessage(text, false);
        }
    }

    private static boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), node, fallback);
    }

    private record HitContext(
            UUID attackerId,
            UUID targetId,
            DamageSource source,
            double healthBefore,
            int level,
            double attackStrength) {
    }
}

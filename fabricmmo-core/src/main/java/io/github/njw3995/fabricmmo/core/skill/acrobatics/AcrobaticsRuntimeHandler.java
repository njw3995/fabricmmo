package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.event.AbilityStateEvent;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.progression.PlayerProgressionContext;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-authoritative Roll, Graceful Roll, Dodge, XP, and exploit prevention. */
public final class AcrobaticsRuntimeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/Acrobatics");
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final ConcurrentHashMap<UUID, PlayerState> PLAYERS = new ConcurrentHashMap<>();
    private static final DodgeXpTracker DODGE_XP = new DodgeXpTracker();
    private static final long RESPAWN_XP_COOLDOWN_MILLIS = 5_000L;

    private AcrobaticsRuntimeHandler() {
    }

    /** Called before vanilla armor reduction, matching Bukkit damage-event Dodge behavior. */
    public static float modifyCombatDamage(
            ServerPlayerEntity player,
            DamageSource source,
            float incomingDamage) {
        if (!available(player) || source.isOf(DamageTypes.FALL) || incomingDamage <= 0.0F) {
            return incomingDamage;
        }
        AcrobaticsSettings settings = FabricMmoFabricRuntime.acrobaticsSettings();
        Entity attacker = source.getAttacker() != null ? source.getAttacker() : source.getSource();
        if (!canDodge(player, attacker, settings)) {
            return incomingDamage;
        }
        double reducedDamage = AcrobaticsDamage.dodgeDamage(
                incomingDamage, settings.dodgeDamageModifier());
        if (AcrobaticsDamage.fatal(player.getHealth(), reducedDamage)) {
            return incomingDamage;
        }
        int level = level(player);
        boolean lucky = allowed(player, PermissionNodes.ACROBATICS_LUCKY, false);
        double chance = settings.dodgeChancePercent(level, lucky);
        if (player.getRandom().nextDouble() >= chance / 100.0D) {
            return incomingDamage;
        }

        if (settings.dodgeParticles()) {
            ServerWorld world = player.getServerWorld();
            world.spawnParticles(
                    ParticleTypes.SMOKE,
                    player.getX(),
                    player.getBodyY(0.5D),
                    player.getZ(),
                    9,
                    0.35D,
                    0.45D,
                    0.35D,
                    0.02D);
        }
        publishActivation(player.getUuid(), CoreAcrobaticsAbilities.DODGE);
        notify(player, "Acrobatics.Combat.Proc");

        if (attacker instanceof MobEntity mob
                && respawnCooldownExpired(player.getUuid(), System.currentTimeMillis())
                && (!settings.dodgeXpFarmingPrevention()
                    || DODGE_XP.tryConsume(mob.getUuid(), System.currentTimeMillis()))) {
            awardXp(
                    player,
                    incomingDamage * settings.dodgeXpModifier(),
                    CoreXpSources.ACROBATICS_DODGE,
                    Map.of("attacker", attacker.getType().toString(), "mechanic", "Dodge"));
        }
        return (float) reducedDamage;
    }

    /** Called after vanilla armor/enchantment reduction and before absorption is consumed. */
    public static float modifyFallDamage(
            ServerPlayerEntity player,
            DamageSource source,
            float finalDamage) {
        if (!available(player) || !source.isOf(DamageTypes.FALL) || finalDamage <= 0.0F) {
            return finalDamage;
        }
        AcrobaticsSettings settings = FabricMmoFabricRuntime.acrobaticsSettings();
        if (!allowed(player, PermissionNodes.ACROBATICS_ROLL, true)) {
            if (allowed(player, PermissionNodes.ACROBATICS, true)) {
                // Upstream awards the untruncated float even when this fall is fatal.
                awardFallXp(player, settings, finalDamage, false, false);
            }
            return finalDamage;
        }

        PlayerState state = PLAYERS.computeIfAbsent(player.getUuid(), ignored -> new PlayerState());
        FallLocationHistory.FallLocation location = location(player);
        boolean exploiting = settings.exploitPrevention()
                && (player.getMainHandStack().isOf(Items.ENDER_PEARL)
                    || player.getOffHandStack().isOf(Items.ENDER_PEARL)
                    || player.hasVehicle()
                    || state.locations.contains(location));
        boolean graceful = player.isSneaking();
        boolean lucky = allowed(player, PermissionNodes.ACROBATICS_LUCKY, false);
        double chance = graceful
                ? settings.gracefulRollChancePercent(level(player), lucky)
                : settings.rollChancePercent(level(player), lucky);
        double modifiedDamage = AcrobaticsDamage.rollDamage(
                finalDamage, settings.effectiveSuccessfulRollDamageThreshold());
        boolean success = !AcrobaticsDamage.fatal(player.getHealth(), modifiedDamage)
                && player.getRandom().nextDouble() < chance / 100.0D;

        if (success) {
            publishActivation(player.getUuid(), CoreAcrobaticsAbilities.ROLL);
            notify(player, graceful ? "Acrobatics.Ability.Proc" : "Acrobatics.Roll.Text");
            playRollSound(player, settings);
        }

        if (success || !AcrobaticsDamage.fatal(player.getHealth(), finalDamage)) {
            // Upstream lengthens the rapid-roll cooldown even when a separate exploit flag
            // suppresses this particular XP award.
            boolean canGainXp = state.throttle.tryConsume(
                    System.currentTimeMillis(), settings.exploitPrevention());
            if (!exploiting && canGainXp) {
                awardFallXp(player, settings, finalDamage, success, true);
            }
            state.locations.add(location);
            return success ? (float) modifiedDamage : finalDamage;
        }
        return finalDamage;
    }

    public static void playerRespawned(UUID playerId) {
        PLAYERS.computeIfAbsent(playerId, ignored -> new PlayerState())
                .lastRespawnMillis = System.currentTimeMillis();
    }

    public static void playerDisconnected(UUID playerId) {
        PLAYERS.remove(playerId);
    }

    public static void clear() {
        PLAYERS.clear();
        DODGE_XP.clear();
    }

    private static boolean canDodge(
            ServerPlayerEntity player,
            Entity attacker,
            AcrobaticsSettings settings) {
        if (player.isBlocking()
                || level(player) < settings.dodgeUnlockLevel()
                || !allowed(player, PermissionNodes.ACROBATICS_DODGE, true)
                || attacker == null) {
            return false;
        }
        /*
         * Pinned mcMMO 2.3.000 CombatUtils passes the damaged player, rather than the attacker,
         * into AcrobaticsManager.canDodge(). The target is therefore always classified as PVP;
         * Enabled_For_PVE and Prevent_Dodge_Lightning do not affect Dodge in that release.
         * Preserve that observable configuration quirk instead of silently correcting upstream.
         */
        return settings.pinnedDodgeCombatEnabled();
    }

    private static void awardFallXp(
            ServerPlayerEntity player,
            AcrobaticsSettings settings,
            double damage,
            boolean successfulRoll,
            boolean truncateLikeRollResult) {
        double xp = AcrobaticsDamage.fallXp(
                damage,
                successfulRoll,
                settings.rollXpModifier(),
                settings.fallXpModifier(),
                hasFeatherFalling(player),
                settings.featherFallingXpMultiplier());
        if (truncateLikeRollResult) {
            xp = AcrobaticsDamage.rollResultXp(xp);
        }
        awardXp(
                player,
                xp,
                CoreXpSources.ACROBATICS_FALL,
                Map.of("mechanic", successfulRoll ? "Roll" : "Fall"));
    }

    private static boolean hasFeatherFalling(ServerPlayerEntity player) {
        if (player.getInventory().getArmorStack(0).isEmpty()) {
            return false;
        }
        RegistryEntry.Reference<Enchantment> enchantment = player.getServerWorld()
                .getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(Enchantments.FEATHER_FALLING)
                .orElse(null);
        return enchantment != null
                && EnchantmentHelper.getLevel(
                        enchantment, player.getInventory().getArmorStack(0)) > 0;
    }

    private static void awardXp(
            ServerPlayerEntity player,
            double xp,
            NamespacedId source,
            Map<String, String> mechanicContext) {
        if (xp <= 0.0D) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        java.util.HashMap<String, String> base = new java.util.HashMap<>(mechanicContext);
        base.put("world", world.getRegistryKey().getValue().toString());
        base.put("upstreamReason", "PVE");
        base.put("upstreamSource", "SELF");
        XpAwardResult result = FabricMmoFabricRuntime.requireApi().progression().award(
                new XpAwardRequest(
                        player.getUuid(),
                        CoreSkills.ACROBATICS,
                        source,
                        xp,
                        PlayerProgressionContext.enrich(
                                player,
                                base,
                                FabricMmoFabricRuntime.progressionSettings(),
                                CoreSkills.ACROBATICS)));
        if (result.status() != XpAwardResult.Status.APPLIED) {
            LOGGER.warn("Acrobatics XP award for {} was not applied: {}",
                    player.getUuid(), result.detail());
        } else if (result.newLevel() > result.oldLevel()) {
            player.sendMessage(net.minecraft.text.Text.literal(
                    "Acrobatics increased to " + result.newLevel() + "."), false);
        }
    }

    private static void notify(ServerPlayerEntity player, String localeKey) {
        if (!SharedServerSystems.running()
                || !SharedServerSystems.require().sessions().get(player.getUuid()).notifications()) {
            return;
        }
        net.minecraft.text.Text message = LegacyText.parse(
                SharedServerSystems.require().locale().text(localeKey));
        AcrobaticsSettings settings = FabricMmoFabricRuntime.acrobaticsSettings();
        if (settings.subSkillMessageActionBar()) {
            player.sendMessage(message, true);
            if (settings.subSkillMessageCopyToChat()) {
                player.sendMessage(message, false);
            }
        } else {
            player.sendMessage(message, false);
        }
    }

    private static void publishActivation(UUID playerId, NamespacedId abilityId) {
        FabricMmoFabricRuntime.requireApi().events().publish(
                new AbilityStateEvent(playerId, abilityId, AbilityStateEvent.State.ACTIVATED));
    }

    private static void playRollSound(
            ServerPlayerEntity player, AcrobaticsSettings settings) {
        if (!settings.rollSoundEnabled() || settings.rollSoundVolume() <= 0.0D) {
            return;
        }
        Identifier soundId = Identifier.tryParse(settings.rollSoundId());
        if (soundId == null) {
            LOGGER.warn("Skipping invalid ROLL_ACTIVATED CustomSoundId: {}",
                    settings.rollSoundId());
            return;
        }
        player.playSoundToPlayer(
                SoundEvent.of(soundId),
                SoundCategory.PLAYERS,
                (float) settings.rollSoundVolume(),
                (float) settings.rollSoundPitch());
    }

    private static boolean respawnCooldownExpired(UUID playerId, long nowMillis) {
        PlayerState state = PLAYERS.get(playerId);
        return state == null || nowMillis - state.lastRespawnMillis >= RESPAWN_XP_COOLDOWN_MILLIS;
    }

    private static int level(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.ACROBATICS).level();
    }

    private static boolean allowed(ServerPlayerEntity player, String permission, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), permission, fallback);
    }

    private static boolean available(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.running()
                && !FabricMmoFabricRuntime.isWorldBlacklisted(player.getServerWorld())
                && allowed(player, PermissionNodes.ACROBATICS, true);
    }

    private static FallLocationHistory.FallLocation location(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        return new FallLocationHistory.FallLocation(
                player.getServerWorld().getRegistryKey().getValue().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ());
    }

    private static final class PlayerState {
        private final FallLocationHistory locations = new FallLocationHistory();
        private final RollXpThrottle throttle = new RollXpThrottle();
        private volatile long lastRespawnMillis;
    }
}

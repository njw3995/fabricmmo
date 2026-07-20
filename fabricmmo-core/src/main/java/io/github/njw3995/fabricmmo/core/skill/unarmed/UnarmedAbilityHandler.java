package io.github.njw3995.fabricmmo.core.skill.unarmed;

import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fabric event bridge for Berserk preparation, activation, expiry, and cooldown. */
public final class UnarmedAbilityHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnarmedAbilityHandler.class.getName());
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final Set<UUID> COOLDOWNS = new HashSet<>();

    private UnarmedAbilityHandler() { }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (hand != Hand.MAIN_HAND || world.isClient()
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !FabricMmoFabricRuntime.running()
                    || FabricMmoFabricRuntime.isWorldBlacklisted(serverPlayer.getServerWorld())
                    || (SharedServerSystems.running()
                        && !SharedServerSystems.require().sessions()
                                .get(serverPlayer.getUuid()).abilityUse())) {
                return TypedActionResult.pass(stack);
            }
            UnarmedSettings settings = FabricMmoFabricRuntime.unarmedSettings();
            if (!UnarmedRuntimeHandler.isUnarmed(stack, settings)) {
                return TypedActionResult.pass(stack);
            }
            if (settings.onlyActivateWhenSneaking() && !serverPlayer.isSneaking()) {
                return TypedActionResult.pass(stack);
            }
            if (!allowed(serverPlayer, PermissionNodes.UNARMED, true)
                    || !allowed(serverPlayer, PermissionNodes.UNARMED_BERSERK, true)) {
                return TypedActionResult.pass(stack);
            }
            prepare(serverPlayer, settings);
            return TypedActionResult.pass(stack);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!FabricMmoFabricRuntime.running()) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tick(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (!FabricMmoFabricRuntime.running()) return;
            UUID id = handler.player.getUuid();
            try {
                if (FabricMmoFabricRuntime.unarmedAbilities().isPrepared(id)
                        || FabricMmoFabricRuntime.unarmedAbilities().isActive(id)) {
                    UnarmedAbilityEvents.cancelled(id);
                }
                FabricMmoFabricRuntime.unarmedAbilities().removeTransient(id);
            } catch (IOException exception) {
                throw new UncheckedIOException("Unable to clear Unarmed ability state", exception);
            }
            COOLDOWNS.remove(id);
        });
    }

    public static boolean activateOnHit(ServerPlayerEntity player) {
        if (!FabricMmoFabricRuntime.running()) return false;
        UnarmedSettings settings = FabricMmoFabricRuntime.unarmedSettings();
        int level = FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.UNARMED).level();
        try {
            UnarmedAbilityController.Activation result =
                    FabricMmoFabricRuntime.unarmedAbilities().activate(
                            player.getUuid(), level, settings,
                            UnarmedPerks.cooldownSeconds(
                                    settings.berserkCooldownSeconds(),
                                    player.getCommandSource(), PERMISSIONS),
                            UnarmedPerks.activationBonusSeconds(
                                    player.getCommandSource(), PERMISSIONS));
            if (result instanceof UnarmedAbilityController.Activation.Activated activated) {
                UnarmedAbilityEvents.activated(player.getUuid());
                message(player, locale("Unarmed.Skills.Berserk.On"),
                        settings.superAbilityNotification(), false, settings);
                playWorldSound(player, settings.abilityActivatedSound());
                notifyNearby(player, settings, true);
                COOLDOWNS.add(player.getUuid());
                return activated.durationSeconds() > 0;
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to activate Berserk", exception);
        }
        return false;
    }

    public static void refresh(ServerPlayerEntity player) {
        try {
            UUID id = player.getUuid();
            if (FabricMmoFabricRuntime.unarmedAbilities().isPrepared(id)
                    || FabricMmoFabricRuntime.unarmedAbilities().isActive(id)) {
                UnarmedAbilityEvents.cancelled(id);
            }
            FabricMmoFabricRuntime.unarmedAbilities().reset(id);
            COOLDOWNS.remove(id);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to refresh Unarmed abilities", exception);
        }
    }

    public static void reset() { COOLDOWNS.clear(); }

    private static void prepare(ServerPlayerEntity player, UnarmedSettings settings) {
        int level = FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.UNARMED).level();
        try {
            UnarmedAbilityController.Preparation result =
                    FabricMmoFabricRuntime.unarmedAbilities().prepare(
                            player.getUuid(), level, settings,
                            UnarmedPerks.cooldownSeconds(
                                    settings.berserkCooldownSeconds(),
                                    player.getCommandSource(), PERMISSIONS));
            if (result == UnarmedAbilityController.Preparation.READY) {
                UnarmedAbilityEvents.prepared(player.getUuid());
                message(player, locale("Unarmed.Ability.Ready"),
                        settings.toolReadyNotification(), true, settings);
                if (settings.abilityMessages()) {
                    playPersonalSound(player, settings.toolReadySound());
                }
            } else if (result == UnarmedAbilityController.Preparation.LOWERED) {
                UnarmedAbilityEvents.cancelled(player.getUuid());
                message(player, locale("Unarmed.Ability.Lower"),
                        settings.toolReadyNotification(), true, settings);
            } else if (result instanceof UnarmedAbilityController.Preparation.Locked locked) {
                message(player, locked(locked.levelsRequired()),
                        settings.abilityCooldownNotification(), false, settings);
            } else if (result instanceof UnarmedAbilityController.Preparation.Cooldown cooldown) {
                message(player, cooldown(cooldown.secondsRemaining()),
                        settings.abilityCooldownNotification(), false, settings);
                COOLDOWNS.add(player.getUuid());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to prepare Berserk", exception);
        }
    }

    private static void tick(ServerPlayerEntity player) {
        try {
            UnarmedAbilityController.TickResult result =
                    FabricMmoFabricRuntime.unarmedAbilities().tick(player.getUuid());
            UnarmedSettings settings = FabricMmoFabricRuntime.unarmedSettings();
            if (result.preparationExpired()) {
                UnarmedAbilityEvents.cancelled(player.getUuid());
                message(player, locale("Unarmed.Ability.Lower"),
                        settings.toolReadyNotification(), true, settings);
            }
            if (result.abilityExpired()) {
                UnarmedAbilityEvents.expired(player.getUuid());
                message(player, locale("Unarmed.Skills.Berserk.Off"),
                        settings.abilityOffNotification(), false, settings);
                notifyNearby(player, settings, false);
            }
            UUID id = player.getUuid();
            int cooldown = UnarmedPerks.cooldownSeconds(
                    settings.berserkCooldownSeconds(), player.getCommandSource(), PERMISSIONS);
            if (COOLDOWNS.contains(id)
                    && FabricMmoFabricRuntime.unarmedAbilities().cooldownRemaining(id, cooldown) == 0) {
                COOLDOWNS.remove(id);
                message(player, locale("Unarmed.Skills.Berserk.Refresh"),
                        settings.abilityRefreshedNotification(), false, settings);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to tick Unarmed abilities", exception);
        }
    }

    private static Text locked(int levelsRequired) {
        if (SharedServerSystems.running()) {
            return LegacyText.parse(SharedServerSystems.require().locale().text(
                    "Skills.AbilityGateRequirementFail",
                    Integer.toString(levelsRequired),
                    SharedServerSystems.require().locale().text("Overhaul.Name.Unarmed")));
        }
        return Text.literal("You require " + levelsRequired
                + " more levels of Unarmed to use this super ability.");
    }

    private static Text cooldown(int seconds) {
        return locale("Skills.TooTired", Integer.toString(seconds));
    }

    private static Text locale(String key, Object... args) {
        if (!SharedServerSystems.running()) return Text.literal(key);
        return LegacyText.parse(SharedServerSystems.require().locale().text(key, args));
    }

    private static void message(
            ServerPlayerEntity player,
            Text text,
            UnarmedSettings.NotificationSetting notification,
            boolean requiresAbilityMessages,
            UnarmedSettings settings) {
        if ((requiresAbilityMessages && !settings.abilityMessages())
                || !notificationsEnabled(player)) {
            return;
        }
        player.sendMessage(text, notification.actionBar());
        if (notification.actionBar() && notification.copyToChat()) {
            player.sendMessage(text, false);
        }
    }

    private static void notifyNearby(
            ServerPlayerEntity player, UnarmedSettings settings, boolean on) {
        if (!settings.notifyNearbyPlayers() || !SharedServerSystems.running()) return;
        String key = on ? "Unarmed.Skills.Berserk.Other.On" : "Unarmed.Skills.Berserk.Other.Off";
        UnarmedSettings.NotificationSetting notification =
                settings.superAbilityAlertOthersNotification();
        for (ServerPlayerEntity other : player.getServerWorld().getPlayers()) {
            if (other != player
                    && other.squaredDistanceTo(player) <= 100.0D
                    && notificationsEnabled(other)) {
                Text text = locale(key, player.getName().getString());
                other.sendMessage(text, notification.actionBar());
                if (notification.actionBar() && notification.copyToChat()) {
                    other.sendMessage(text, false);
                }
            }
        }
    }

    private static boolean notificationsEnabled(ServerPlayerEntity player) {
        return !SharedServerSystems.running()
                || SharedServerSystems.require().sessions()
                        .get(player.getUuid()).notifications();
    }

    private static void playPersonalSound(
            ServerPlayerEntity player, UnarmedSettings.SoundSetting sound) {
        SoundEvent event = resolve(sound);
        if (event == null) return;
        player.playSoundToPlayer(
                event,
                SoundCategory.PLAYERS,
                (float) sound.volume(),
                (float) sound.pitch());
    }

    private static void playWorldSound(
            ServerPlayerEntity player, UnarmedSettings.SoundSetting sound) {
        SoundEvent event = resolve(sound);
        if (event == null) return;
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                event,
                SoundCategory.PLAYERS,
                (float) sound.volume(),
                (float) sound.pitch());
    }

    private static SoundEvent resolve(UnarmedSettings.SoundSetting sound) {
        if (!sound.enabled() || sound.volume() <= 0.0D) return null;
        Identifier id = Identifier.tryParse(sound.id());
        if (id == null) {
            LOGGER.warn("Skipping invalid configured Unarmed sound ID: {}", sound.id());
            return null;
        }
        return SoundEvent.of(id);
    }

    private static boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), node, fallback);
    }

}

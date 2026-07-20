package io.github.njw3995.fabricmmo.core.skill.swords;

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
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fabric event bridge for Serrated Strikes preparation, activation, expiry, and cooldown. */
public final class SwordsAbilityHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwordsAbilityHandler.class.getName());
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final Set<UUID> COOLDOWNS = new HashSet<>();

    private SwordsAbilityHandler() { }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (hand != Hand.MAIN_HAND || world.isClient()
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !stack.isIn(ItemTags.SWORDS)
                    || !FabricMmoFabricRuntime.running()
                    || FabricMmoFabricRuntime.isWorldBlacklisted(serverPlayer.getServerWorld())
                    || (SharedServerSystems.running()
                        && !SharedServerSystems.require().sessions()
                                .get(serverPlayer.getUuid()).abilityUse())) {
                return TypedActionResult.pass(stack);
            }
            SwordsSettings settings = FabricMmoFabricRuntime.swordsSettings();
            if (settings.onlyActivateWhenSneaking() && !serverPlayer.isSneaking()) {
                return TypedActionResult.pass(stack);
            }
            if (!allowed(serverPlayer, PermissionNodes.SWORDS, true)
                    || !allowed(serverPlayer, PermissionNodes.SWORDS_SERRATED_STRIKES, true)) {
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
                if (FabricMmoFabricRuntime.swordsAbilities().isPrepared(id)
                        || FabricMmoFabricRuntime.swordsAbilities().isActive(id)) {
                    SwordsAbilityEvents.cancelled(id);
                }
                FabricMmoFabricRuntime.swordsAbilities().removeTransient(id);
            } catch (IOException exception) {
                throw new UncheckedIOException("Unable to clear Swords ability state", exception);
            }
            COOLDOWNS.remove(id);
        });
    }

    public static boolean activateOnHit(ServerPlayerEntity player) {
        if (!FabricMmoFabricRuntime.running()) return false;
        SwordsSettings settings = FabricMmoFabricRuntime.swordsSettings();
        int level = FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.SWORDS).level();
        try {
            SwordsAbilityController.Activation result =
                    FabricMmoFabricRuntime.swordsAbilities().activate(
                            player.getUuid(), level, settings,
                            SwordsPerks.cooldownSeconds(
                                    settings.serratedCooldownSeconds(),
                                    player.getCommandSource(), PERMISSIONS),
                            SwordsPerks.activationBonusSeconds(
                                    player.getCommandSource(), PERMISSIONS));
            if (result instanceof SwordsAbilityController.Activation.Activated activated) {
                SwordsAbilityEvents.activated(player.getUuid());
                message(player, locale("Swords.Skills.SS.On"),
                        settings.superAbilityNotification(), false, settings);
                playWorldSound(player, settings.abilityActivatedSound());
                notifyNearby(player, settings, true);
                COOLDOWNS.add(player.getUuid());
                return activated.durationSeconds() > 0;
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to activate Serrated Strikes", exception);
        }
        return false;
    }

    public static void refresh(ServerPlayerEntity player) {
        try {
            UUID id = player.getUuid();
            if (FabricMmoFabricRuntime.swordsAbilities().isPrepared(id)
                    || FabricMmoFabricRuntime.swordsAbilities().isActive(id)) {
                SwordsAbilityEvents.cancelled(id);
            }
            FabricMmoFabricRuntime.swordsAbilities().reset(id);
            COOLDOWNS.remove(id);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to refresh Swords abilities", exception);
        }
    }

    public static void reset() { COOLDOWNS.clear(); }

    private static void prepare(ServerPlayerEntity player, SwordsSettings settings) {
        int level = FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.SWORDS).level();
        try {
            SwordsAbilityController.Preparation result =
                    FabricMmoFabricRuntime.swordsAbilities().prepare(
                            player.getUuid(), level, settings,
                            SwordsPerks.cooldownSeconds(
                                    settings.serratedCooldownSeconds(),
                                    player.getCommandSource(), PERMISSIONS));
            if (result == SwordsAbilityController.Preparation.READY) {
                SwordsAbilityEvents.prepared(player.getUuid());
                message(player, locale("Swords.Ability.Ready"),
                        settings.toolReadyNotification(), true, settings);
                if (settings.abilityMessages()) {
                    playPersonalSound(player, settings.toolReadySound());
                }
            } else if (result == SwordsAbilityController.Preparation.LOWERED) {
                SwordsAbilityEvents.cancelled(player.getUuid());
                message(player, locale("Swords.Ability.Lower"),
                        settings.toolReadyNotification(), true, settings);
            } else if (result instanceof SwordsAbilityController.Preparation.Locked locked) {
                message(player, locked(locked.levelsRequired()),
                        settings.abilityCooldownNotification(), false, settings);
            } else if (result instanceof SwordsAbilityController.Preparation.Cooldown cooldown) {
                message(player, cooldown(cooldown.secondsRemaining()),
                        settings.abilityCooldownNotification(), false, settings);
                COOLDOWNS.add(player.getUuid());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to prepare Serrated Strikes", exception);
        }
    }

    private static void tick(ServerPlayerEntity player) {
        try {
            SwordsAbilityController.TickResult result =
                    FabricMmoFabricRuntime.swordsAbilities().tick(player.getUuid());
            SwordsSettings settings = FabricMmoFabricRuntime.swordsSettings();
            if (result.preparationExpired()) {
                SwordsAbilityEvents.cancelled(player.getUuid());
                message(player, locale("Swords.Ability.Lower"),
                        settings.toolReadyNotification(), true, settings);
            }
            if (result.abilityExpired()) {
                SwordsAbilityEvents.expired(player.getUuid());
                message(player, locale("Swords.Skills.SS.Off"),
                        settings.abilityOffNotification(), false, settings);
                notifyNearby(player, settings, false);
            }
            UUID id = player.getUuid();
            int cooldown = SwordsPerks.cooldownSeconds(
                    settings.serratedCooldownSeconds(), player.getCommandSource(), PERMISSIONS);
            if (COOLDOWNS.contains(id)
                    && FabricMmoFabricRuntime.swordsAbilities().cooldownRemaining(id, cooldown) == 0) {
                COOLDOWNS.remove(id);
                message(player, locale("Swords.Skills.SS.Refresh"),
                        settings.abilityRefreshedNotification(), false, settings);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to tick Swords abilities", exception);
        }
    }

    private static Text locked(int levelsRequired) {
        if (SharedServerSystems.running()) {
            return LegacyText.parse(SharedServerSystems.require().locale().text(
                    "Skills.AbilityGateRequirementFail",
                    Integer.toString(levelsRequired),
                    SharedServerSystems.require().locale().text("Overhaul.Name.Swords")));
        }
        return Text.literal("You require " + levelsRequired
                + " more levels of Swords to use this super ability.");
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
            SwordsSettings.NotificationSetting notification,
            boolean requiresAbilityMessages,
            SwordsSettings settings) {
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
            ServerPlayerEntity player, SwordsSettings settings, boolean on) {
        if (!settings.notifyNearbyPlayers() || !SharedServerSystems.running()) return;
        String key = on ? "Swords.Skills.SS.Other.On" : "Swords.Skills.SS.Other.Off";
        SwordsSettings.NotificationSetting notification =
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
            ServerPlayerEntity player, SwordsSettings.SoundSetting sound) {
        SoundEvent event = resolve(sound);
        if (event == null) return;
        player.playSoundToPlayer(
                event,
                SoundCategory.PLAYERS,
                (float) sound.volume(),
                (float) sound.pitch());
    }

    private static void playWorldSound(
            ServerPlayerEntity player, SwordsSettings.SoundSetting sound) {
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

    private static SoundEvent resolve(SwordsSettings.SoundSetting sound) {
        if (!sound.enabled() || sound.volume() <= 0.0D) return null;
        Identifier id = Identifier.tryParse(sound.id());
        if (id == null) {
            LOGGER.warn("Skipping invalid configured Swords sound ID: {}", sound.id());
            return null;
        }
        return SoundEvent.of(id);
    }

    private static boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), node, fallback);
    }

}

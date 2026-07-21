package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import io.github.njw3995.fabricmmo.core.skill.gathering.ToolActivationBlockFilter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

/** Fabric bridge for Green Terra preparation, activation, conversion, expiry, and refresh. */
public final class HerbalismAbilityHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final Set<UUID> COOLDOWNS = new HashSet<>();
    private static final Set<UUID> PREPARED_FROM_BLOCK_INTERACTION = new HashSet<>();

    private HerbalismAbilityHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND
                    || world.isClient()
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !canPrepare(serverPlayer)
                    || !ToolActivationBlockFilter.canActivateTools(
                            world.getBlockState(hit.getBlockPos()))) {
                return ActionResult.PASS;
            }
            prepare(serverPlayer);
            PREPARED_FROM_BLOCK_INTERACTION.add(serverPlayer.getUuid());
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (hand != Hand.MAIN_HAND
                    || world.isClient()
                    || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return TypedActionResult.pass(stack);
            }
            if (PREPARED_FROM_BLOCK_INTERACTION.remove(serverPlayer.getUuid())) {
                return TypedActionResult.pass(stack);
            }
            if (canPrepare(serverPlayer)) {
                prepare(serverPlayer);
            }
            return TypedActionResult.pass(stack);
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (hand != Hand.MAIN_HAND
                    || world.isClient()
                    || !(world instanceof ServerWorld serverWorld)
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !available(serverPlayer)) {
                return ActionResult.PASS;
            }
            ItemStack tool = serverPlayer.getMainHandStack();
            BlockState state = serverWorld.getBlockState(pos);
            var extension = FabricMmoFabricRuntime.gatheringContentFor(CoreSkills.HERBALISM, state);
            boolean validTool = extension
                    .map(definition -> FabricMmoFabricRuntime.gatheringContentResolver()
                            .validTool(definition, tool))
                    .orElseGet(() -> tool.isIn(ItemTags.HOES));
            boolean activeEligible = extension.map(definition -> definition.activeAbility())
                    .orElse(true);
            if (!validTool
                    || !activeEligible
                    || FabricMmoFabricRuntime.herbalismXpFor(state) <= 0
                    || !PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.HERBALISM, true)
                    || !PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(),
                            PermissionNodes.HERBALISM_GREEN_TERRA,
                            true)) {
                return ActionResult.PASS;
            }
            int level = FabricMmoFabricRuntime.requireApi().progression()
                    .query(serverPlayer.getUuid(), CoreSkills.HERBALISM).level();
            activate(serverPlayer, level);
            if (isActive(serverPlayer.getUuid()) && HerbalismConversions.canGreen(state)) {
                if (!HerbalismInventory.removeOne(serverPlayer, Items.WHEAT_SEEDS)) {
                    message(serverPlayer, FabricMmoFabricRuntime.herbalismSettings(),
                            HerbalismMessages.needMore("Wheat Seeds"));
                    return ActionResult.PASS;
                }
                if (FabricMmoFabricRuntime.requireApi().protection().canBreak(
                        serverPlayer.getUuid(),
                        serverWorld.getRegistryKey().getValue().toString(),
                        pos.getX(), pos.getY(), pos.getZ())
                        && HerbalismConversions.convertGreen(serverWorld, pos, state)) {
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PREPARED_FROM_BLOCK_INTERACTION.clear();
            if (!FabricMmoFabricRuntime.running()) {
                return;
            }
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tick(player);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (!FabricMmoFabricRuntime.running()) {
                return;
            }
            UUID playerId = handler.player.getUuid();
            try {
                if (FabricMmoFabricRuntime.herbalismAbilities().isPrepared(playerId)
                        || FabricMmoFabricRuntime.herbalismAbilities().isActive(playerId)) {
                    HerbalismAbilityEvents.cancelled(playerId, CoreHerbalismAbilities.GREEN_TERRA);
                }
            } catch (IOException exception) {
                throw new UncheckedIOException("Unable to clear Herbalism ability state", exception);
            }
            FabricMmoFabricRuntime.herbalismAbilities().removeTransient(playerId);
        });
    }

    public static void reset() {
        COOLDOWNS.clear();
        PREPARED_FROM_BLOCK_INTERACTION.clear();
    }

    private static boolean canPrepare(ServerPlayerEntity player) {
        return available(player)
                && player.getMainHandStack().isIn(ItemTags.HOES)
                && (!FabricMmoFabricRuntime.herbalismSettings().onlyActivateWhenSneaking()
                        || player.isSneaking())
                && PERMISSIONS.hasPermission(
                        player.getCommandSource(), PermissionNodes.HERBALISM, true)
                && PERMISSIONS.hasPermission(
                        player.getCommandSource(), PermissionNodes.HERBALISM_GREEN_TERRA, true);
    }

    private static boolean available(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.running()
                && !player.isCreative()
                && (!SharedServerSystems.running()
                        || SharedServerSystems.require().sessions()
                                .get(player.getUuid()).abilityUse())
                && !FabricMmoFabricRuntime.isWorldBlacklisted(player.getServerWorld());
    }

    private static void prepare(ServerPlayerEntity player) {
        HerbalismSettings settings = FabricMmoFabricRuntime.herbalismSettings();
        int level = FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.HERBALISM).level();
        int cooldown = HerbalismPerks.cooldownSeconds(
                settings.greenTerraCooldownSeconds(), player.getCommandSource(), PERMISSIONS);
        try {
            HerbalismAbilityController.Preparation result =
                    FabricMmoFabricRuntime.herbalismAbilities().prepare(
                            player.getUuid(), level, settings, cooldown);
            if (result == HerbalismAbilityController.Preparation.READY) {
                HerbalismAbilityEvents.prepared(player.getUuid(), CoreHerbalismAbilities.GREEN_TERRA);
                message(player, settings, HerbalismMessages.hoeReady());
                sound(player, SoundEvents.ITEM_ARMOR_EQUIP_GOLD, 0.4F);
            } else if (result instanceof HerbalismAbilityController.Preparation.Locked locked) {
                message(player, settings, HerbalismMessages.locked(locked.levelsRequired()));
            } else if (result instanceof HerbalismAbilityController.Preparation.Cooldown tired) {
                message(player, settings, HerbalismMessages.cooldown(tired.secondsRemaining()));
                sound(player, SoundEvents.ENTITY_VILLAGER_NO, 1.7F);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to prepare Green Terra", exception);
        }
    }

    private static void activate(ServerPlayerEntity player, int level) {
        HerbalismSettings settings = FabricMmoFabricRuntime.herbalismSettings();
        int cooldown = HerbalismPerks.cooldownSeconds(
                settings.greenTerraCooldownSeconds(), player.getCommandSource(), PERMISSIONS);
        try {
            HerbalismAbilityController.Activation result =
                    FabricMmoFabricRuntime.herbalismAbilities().activate(
                            player.getUuid(), level, settings, cooldown,
                            HerbalismPerks.activationBonusSeconds(
                                    player.getCommandSource(), PERMISSIONS));
            if (result instanceof HerbalismAbilityController.Activation.Activated) {
                COOLDOWNS.add(player.getUuid());
                HerbalismAbilityEvents.activated(player.getUuid(), CoreHerbalismAbilities.GREEN_TERRA);
                message(player, settings, HerbalismMessages.activated());
                notifyNearby(player, settings, true);
                sound(player, SoundEvents.BLOCK_GRASS_BREAK, 1.0F);
            } else if (result instanceof HerbalismAbilityController.Activation.Locked locked) {
                message(player, settings, HerbalismMessages.locked(locked.levelsRequired()));
            } else if (result instanceof HerbalismAbilityController.Activation.Cooldown tired) {
                message(player, settings, HerbalismMessages.cooldown(tired.secondsRemaining()));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to activate Green Terra", exception);
        }
    }

    private static void tick(ServerPlayerEntity player) {
        try {
            HerbalismSettings settings = FabricMmoFabricRuntime.herbalismSettings();
            HerbalismAbilityController.TickResult result =
                    FabricMmoFabricRuntime.herbalismAbilities().tick(player.getUuid());
            if (result.preparationExpired()) {
                HerbalismAbilityEvents.cancelled(player.getUuid(), CoreHerbalismAbilities.GREEN_TERRA);
                message(player, settings, HerbalismMessages.hoeLowered());
            }
            if (result.abilityExpired()) {
                HerbalismAbilityEvents.expired(player.getUuid(), CoreHerbalismAbilities.GREEN_TERRA);
                message(player, settings, HerbalismMessages.expired());
                notifyNearby(player, settings, false);
            }
            int cooldown = HerbalismPerks.cooldownSeconds(
                    settings.greenTerraCooldownSeconds(), player.getCommandSource(), PERMISSIONS);
            if (COOLDOWNS.contains(player.getUuid())
                    && FabricMmoFabricRuntime.herbalismAbilities()
                            .cooldownRemaining(player.getUuid(), cooldown) == 0) {
                COOLDOWNS.remove(player.getUuid());
                message(player, settings, HerbalismMessages.refreshed());
                sound(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to tick Herbalism abilities", exception);
        }
    }

    static boolean isActive(UUID playerId) {
        try {
            return FabricMmoFabricRuntime.herbalismAbilities().isActive(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Green Terra state", exception);
        }
    }

    private static void message(
            ServerPlayerEntity player,
            HerbalismSettings settings,
            net.minecraft.text.Text text) {
        if (settings.abilityMessages()
                && (!SharedServerSystems.running()
                        || SharedServerSystems.require().sessions()
                                .get(player.getUuid()).notifications())) {
            player.sendMessage(text, true);
        }
    }

    private static void notifyNearby(
            ServerPlayerEntity player,
            HerbalismSettings settings,
            boolean activated) {
        if (!settings.notifyNearbyPlayers()) {
            return;
        }
        for (ServerPlayerEntity other : player.getServerWorld().getPlayers()) {
            if (other != player && other.squaredDistanceTo(player) <= 100.0D) {
                other.sendMessage(activated
                        ? HerbalismMessages.activatedOther(player.getName().getString())
                        : HerbalismMessages.expiredOther(player.getName().getString()), false);
            }
        }
    }

    private static void sound(
            ServerPlayerEntity player,
            net.minecraft.registry.entry.RegistryEntry<net.minecraft.sound.SoundEvent> sound,
            float pitch) {
        sound(player, sound.value(), pitch);
    }

    private static void sound(
            ServerPlayerEntity player,
            net.minecraft.sound.SoundEvent sound,
            float pitch) {
        player.getServerWorld().playSound(
                null, player.getBlockPos(), sound, SoundCategory.PLAYERS, 1.0F, pitch);
    }
}

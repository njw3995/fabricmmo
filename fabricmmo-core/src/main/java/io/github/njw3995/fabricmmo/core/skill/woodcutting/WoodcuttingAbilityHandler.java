package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.core.block.BlockLocation;
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
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/** Fabric bridge for Tree Feller preparation/activation, Leaf Blower, expiry and refresh. */
public final class WoodcuttingAbilityHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final Set<UUID> TREE_FELLER_COOLDOWNS = new HashSet<>();
    private static final Set<UUID> PREPARED_FROM_BLOCK_INTERACTION = new HashSet<>();

    private WoodcuttingAbilityHandler() {
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
            prepare(serverPlayer, FabricMmoFabricRuntime.woodcuttingSettings());
            PREPARED_FROM_BLOCK_INTERACTION.add(serverPlayer.getUuid());
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            var stack = player.getStackInHand(hand);
            if (hand != Hand.MAIN_HAND
                    || world.isClient()
                    || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return TypedActionResult.pass(stack);
            }
            if (PREPARED_FROM_BLOCK_INTERACTION.remove(serverPlayer.getUuid())) {
                return TypedActionResult.pass(stack);
            }
            if (canPrepare(serverPlayer)) {
                prepare(serverPlayer, FabricMmoFabricRuntime.woodcuttingSettings());
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
            var tool = serverPlayer.getMainHandStack();
            if (!tool.isIn(ItemTags.AXES)) {
                return ActionResult.PASS;
            }
            BlockState state = serverWorld.getBlockState(pos);
            int level = FabricMmoFabricRuntime.requireApi().progression()
                    .query(serverPlayer.getUuid(), CoreSkills.WOODCUTTING).level();
            WoodcuttingSettings settings = FabricMmoFabricRuntime.woodcuttingSettings();

            if (WoodcuttingBlockClassifier.isNonWoodTreePart(state)
                    && level >= settings.leafBlowerUnlockLevel()
                    && PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.WOODCUTTING, true)
                    && PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(),
                            PermissionNodes.WOODCUTTING_LEAF_BLOWER,
                            true)
                    && FabricMmoFabricRuntime.requireApi().protection().canBreak(
                            serverPlayer.getUuid(),
                            serverWorld.getRegistryKey().getValue().toString(),
                            pos.getX(), pos.getY(), pos.getZ())) {
                if (serverPlayer.interactionManager.tryBreakBlock(pos)) {
                    serverWorld.playSound(
                            null,
                            pos,
                            SoundEvents.ENTITY_ITEM_PICKUP,
                            SoundCategory.BLOCKS,
                            1.0F,
                            popPitch(serverWorld));
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (TreeFellerContext.active()
                    || !(world instanceof ServerWorld serverWorld)
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !available(serverPlayer)) {
                return true;
            }
            TreeFellerContext.clearStartingBreak(serverPlayer.getUuid());
            if (!serverPlayer.getMainHandStack().isIn(ItemTags.AXES)
                    || FabricMmoFabricRuntime.woodcuttingXpFor(
                            WoodcuttingBlockClassifier.id(state)) <= 0
                    || !PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.WOODCUTTING, true)
                    || !PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(),
                            PermissionNodes.WOODCUTTING_TREE_FELLER,
                            true)) {
                return true;
            }

            WoodcuttingSettings settings = FabricMmoFabricRuntime.woodcuttingSettings();
            int level = FabricMmoFabricRuntime.requireApi().progression()
                    .query(serverPlayer.getUuid(), CoreSkills.WOODCUTTING).level();
            activate(serverPlayer, settings, level);
            if (!FabricMmoFabricRuntime.isTreeFellerActive(serverPlayer.getUuid())) {
                return true;
            }

            String worldId = serverWorld.getRegistryKey().getValue().toString();
            BlockLocation startingLocation = new BlockLocation(
                    worldId, pos.getX(), pos.getY(), pos.getZ());
            if (!FabricMmoFabricRuntime.requireApi().protection().canBreak(
                    serverPlayer.getUuid(), worldId, pos.getX(), pos.getY(), pos.getZ())
                    || FabricMmoFabricRuntime.isPlayerPlaced(startingLocation)) {
                return true;
            }

            if (settings.treeFellerSounds()) {
                serverWorld.playSound(
                        null,
                        pos,
                        SoundEvents.BLOCK_FIRE_EXTINGUISH,
                        SoundCategory.BLOCKS,
                        1.0F,
                        fizzPitch(serverWorld));
            }
            TreeFellerContext.markStartingBreak(serverPlayer.getUuid(), startingLocation);
            WoodcuttingTreeFellerProcessor.process(serverWorld, serverPlayer, pos);
            return true;
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
                if (FabricMmoFabricRuntime.woodcuttingAbilities().isPrepared(playerId)
                        || FabricMmoFabricRuntime.woodcuttingAbilities().isActive(playerId)) {
                    WoodcuttingAbilityEvents.cancelled(
                            playerId, CoreWoodcuttingAbilities.TREE_FELLER);
                }
            } catch (IOException exception) {
                throw new UncheckedIOException(
                        "Unable to clear Woodcutting ability state", exception);
            }
            FabricMmoFabricRuntime.woodcuttingAbilities().removeTransient(playerId);
            TreeFellerContext.clearStartingBreak(playerId);
        });
    }

    public static void reset() {
        TREE_FELLER_COOLDOWNS.clear();
        PREPARED_FROM_BLOCK_INTERACTION.clear();
        TreeFellerContext.reset();
    }

    public static void refresh(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        try {
            if (FabricMmoFabricRuntime.woodcuttingAbilities().isPrepared(playerId)
                    || FabricMmoFabricRuntime.woodcuttingAbilities().isActive(playerId)) {
                WoodcuttingAbilityEvents.cancelled(
                        playerId, CoreWoodcuttingAbilities.TREE_FELLER);
            }
            FabricMmoFabricRuntime.woodcuttingAbilities().reset(playerId);
            TREE_FELLER_COOLDOWNS.remove(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to refresh Woodcutting abilities", exception);
        }
    }


    private static boolean canPrepare(ServerPlayerEntity player) {
        if (!available(player)
                || !player.getMainHandStack().isIn(ItemTags.AXES)
                || (!player.getOffHandStack().isEmpty()
                        && !player.hasVehicle()
                        && !player.isSneaking())) {
            return false;
        }
        WoodcuttingSettings settings = FabricMmoFabricRuntime.woodcuttingSettings();
        return (!settings.onlyActivateWhenSneaking() || player.isSneaking())
                && PERMISSIONS.hasPermission(
                        player.getCommandSource(), PermissionNodes.WOODCUTTING, true)
                && PERMISSIONS.hasPermission(
                        player.getCommandSource(),
                        PermissionNodes.WOODCUTTING_TREE_FELLER,
                        true);
    }

    private static boolean available(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.running()
                && !player.isCreative()
                && (!SharedServerSystems.running()
                        || SharedServerSystems.require().sessions()
                                .get(player.getUuid()).abilityUse())
                && !FabricMmoFabricRuntime.isWorldBlacklisted(player.getServerWorld());
    }

    private static void prepare(ServerPlayerEntity player, WoodcuttingSettings settings) {
        try {
            WoodcuttingAbilityController.Preparation result =
                    FabricMmoFabricRuntime.woodcuttingAbilities().prepare(
                            player.getUuid(), settings);
            if (result == WoodcuttingAbilityController.Preparation.READY) {
                WoodcuttingAbilityEvents.prepared(
                        player.getUuid(), CoreWoodcuttingAbilities.TREE_FELLER);
                int cooldown = WoodcuttingPerks.cooldownSeconds(
                        settings.treeFellerCooldownSeconds(),
                        player.getCommandSource(),
                        PERMISSIONS);
                int remaining = FabricMmoFabricRuntime.woodcuttingAbilities()
                        .cooldownRemaining(player.getUuid(), cooldown);
                message(player, settings, remaining > 0 && lookingAtWood(player)
                        ? WoodcuttingMessages.axeReadyCooldown(remaining)
                        : WoodcuttingMessages.axeReady());
                sound(player, SoundEvents.ITEM_ARMOR_EQUIP_GOLD, 1.0F);
            } else if (result == WoodcuttingAbilityController.Preparation.LOWERED) {
                WoodcuttingAbilityEvents.cancelled(
                        player.getUuid(), CoreWoodcuttingAbilities.TREE_FELLER);
                message(player, settings, WoodcuttingMessages.axeLowered());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to prepare Tree Feller", exception);
        }
    }

    private static void activate(
            ServerPlayerEntity player,
            WoodcuttingSettings settings,
            int level) {
        try {
            int cooldown = WoodcuttingPerks.cooldownSeconds(
                    settings.treeFellerCooldownSeconds(),
                    player.getCommandSource(),
                    PERMISSIONS);
            WoodcuttingAbilityController.Activation result =
                    FabricMmoFabricRuntime.woodcuttingAbilities().activate(
                            player.getUuid(),
                            level,
                            settings,
                            cooldown,
                            WoodcuttingPerks.activationBonusSeconds(
                                    player.getCommandSource(), PERMISSIONS));
            if (result instanceof WoodcuttingAbilityController.Activation.Activated) {
                TREE_FELLER_COOLDOWNS.add(player.getUuid());
                WoodcuttingAbilityEvents.activated(
                        player.getUuid(), CoreWoodcuttingAbilities.TREE_FELLER);
                message(player, settings, WoodcuttingMessages.treeFellerActivated());
                notifyNearby(player, settings, true);
                sound(player, SoundEvents.ITEM_TRIDENT_RIPTIDE_3, 1.0F);
            } else if (result instanceof WoodcuttingAbilityController.Activation.Locked locked) {
                message(player, settings, WoodcuttingMessages.locked(locked.levelsRequired()));
            } else if (result instanceof WoodcuttingAbilityController.Activation.Cooldown tired) {
                message(player, settings, WoodcuttingMessages.cooldown(tired.secondsRemaining()));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to activate Tree Feller", exception);
        }
    }

    private static void tick(ServerPlayerEntity player) {
        try {
            WoodcuttingAbilityController.TickResult result =
                    FabricMmoFabricRuntime.woodcuttingAbilities().tick(player.getUuid());
            WoodcuttingSettings settings = FabricMmoFabricRuntime.woodcuttingSettings();
            if (result.preparationExpired()) {
                WoodcuttingAbilityEvents.cancelled(
                        player.getUuid(), CoreWoodcuttingAbilities.TREE_FELLER);
                message(player, settings, WoodcuttingMessages.axeLowered());
            }
            if (result.abilityExpired()) {
                WoodcuttingAbilityEvents.expired(
                        player.getUuid(), CoreWoodcuttingAbilities.TREE_FELLER);
                message(player, settings, WoodcuttingMessages.treeFellerExpired());
                notifyNearby(player, settings, false);
            }
            UUID playerId = player.getUuid();
            int cooldown = WoodcuttingPerks.cooldownSeconds(
                    settings.treeFellerCooldownSeconds(),
                    player.getCommandSource(),
                    PERMISSIONS);
            if (TREE_FELLER_COOLDOWNS.contains(playerId)
                    && FabricMmoFabricRuntime.woodcuttingAbilities()
                            .cooldownRemaining(playerId, cooldown) == 0) {
                TREE_FELLER_COOLDOWNS.remove(playerId);
                message(player, settings, WoodcuttingMessages.treeFellerRefreshed());
                sound(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to tick Woodcutting abilities", exception);
        }
    }

    private static boolean lookingAtWood(ServerPlayerEntity player) {
        HitResult hit = player.raycast(100.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return false;
        }
        BlockState state = player.getServerWorld().getBlockState(blockHit.getBlockPos());
        return FabricMmoFabricRuntime.woodcuttingXpFor(WoodcuttingBlockClassifier.id(state)) > 0;
    }

    private static float fizzPitch(ServerWorld world) {
        return 2.6F + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.8F;
    }

    private static float popPitch(ServerWorld world) {
        return ((world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7F + 1.0F)
                * 2.0F;
    }

    private static void message(
            ServerPlayerEntity player,
            WoodcuttingSettings settings,
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
            WoodcuttingSettings settings,
            boolean activated) {
        if (!settings.notifyNearbyPlayers()) {
            return;
        }
        for (ServerPlayerEntity other : player.getServerWorld().getPlayers()) {
            if (other != player && other.squaredDistanceTo(player) <= 100.0D) {
                other.sendMessage(
                        activated
                                ? WoodcuttingMessages.treeFellerActivatedOther(
                                        player.getName().getString())
                                : WoodcuttingMessages.treeFellerExpiredOther(
                                        player.getName().getString()),
                        false);
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
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                sound,
                SoundCategory.PLAYERS,
                1.0F,
                pitch);
    }
}

package io.github.njw3995.fabricmmo.core.skill.excavation;

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
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

/** Fabric bridge for Giga Drill Breaker preparation, activation, expiry and refresh. */
public final class ExcavationAbilityHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final Set<UUID> COOLDOWNS = new HashSet<>();
    private static final Set<UUID> PREPARED_FROM_BLOCK_INTERACTION = new HashSet<>();

    private ExcavationAbilityHandler() {
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
            NamespacedId blockId = NamespacedId.parse(
                    Registries.BLOCK.getId(state.getBlock()).toString());
            if (!tool.isIn(ItemTags.SHOVELS)
                    || FabricMmoFabricRuntime.excavationXpFor(blockId) <= 0
                    || !PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.EXCAVATION, true)
                    || !PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(),
                            PermissionNodes.EXCAVATION_GIGA_DRILL_BREAKER,
                            true)) {
                return ActionResult.PASS;
            }
            int level = FabricMmoFabricRuntime.requireApi().progression()
                    .query(serverPlayer.getUuid(), CoreSkills.EXCAVATION).level();
            activate(serverPlayer, level);
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

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                GigaDrillAttributeBoost.cleanupInventory(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (!FabricMmoFabricRuntime.running()) {
                return;
            }
            UUID playerId = handler.player.getUuid();
            try {
                if (FabricMmoFabricRuntime.excavationAbilities().isPrepared(playerId)
                        || FabricMmoFabricRuntime.excavationAbilities().isActive(playerId)) {
                    ExcavationAbilityEvents.cancelled(
                            playerId, CoreExcavationAbilities.GIGA_DRILL_BREAKER);
                }
            } catch (IOException exception) {
                throw new UncheckedIOException("Unable to clear Excavation ability state", exception);
            }
            GigaDrillAttributeBoost.clear(handler.player);
            FabricMmoFabricRuntime.excavationAbilities().removeTransient(playerId);
        });
    }

    public static void reset() {
        COOLDOWNS.clear();
        PREPARED_FROM_BLOCK_INTERACTION.clear();
        GigaDrillAttributeBoost.reset();
    }

    public static void restoreDroppedTool(ServerPlayerEntity player, ItemStack stack) {
        if (FabricMmoFabricRuntime.running()) {
            GigaDrillAttributeBoost.restoreDroppedStack(player, stack);
        }
    }

    /**
     * Removes Giga Drill Breaker's temporary Efficiency before Repair or Salvage mutates
     * the item's real enchantments. The ability may reapply its temporary bonus on the next
     * tick, but expiry will then restore the post-anvil enchantment state.
     */
    public static void prepareToolForUtilityAnvil(ServerPlayerEntity player) {
        if (FabricMmoFabricRuntime.running()) {
            GigaDrillAttributeBoost.clear(player);
        }
    }

    private static boolean canPrepare(ServerPlayerEntity player) {
        return available(player)
                && player.getMainHandStack().isIn(ItemTags.SHOVELS)
                && (!FabricMmoFabricRuntime.excavationSettings().onlyActivateWhenSneaking()
                        || player.isSneaking())
                && PERMISSIONS.hasPermission(
                        player.getCommandSource(), PermissionNodes.EXCAVATION, true)
                && PERMISSIONS.hasPermission(
                        player.getCommandSource(),
                        PermissionNodes.EXCAVATION_GIGA_DRILL_BREAKER,
                        true);
    }

    private static boolean available(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.running()
                && (!SharedServerSystems.running()
                        || SharedServerSystems.require().sessions()
                                .get(player.getUuid()).abilityUse())
                && !FabricMmoFabricRuntime.isWorldBlacklisted(player.getServerWorld());
    }

    private static void prepare(ServerPlayerEntity player) {
        ExcavationSettings settings = FabricMmoFabricRuntime.excavationSettings();
        int level = FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.EXCAVATION).level();
        int cooldown = ExcavationPerks.cooldownSeconds(
                settings.gigaDrillCooldownSeconds(), player.getCommandSource(), PERMISSIONS);
        try {
            ExcavationAbilityController.Preparation result =
                    FabricMmoFabricRuntime.excavationAbilities().prepare(
                            player.getUuid(), level, settings, cooldown);
            if (result == ExcavationAbilityController.Preparation.READY) {
                ExcavationAbilityEvents.prepared(
                        player.getUuid(), CoreExcavationAbilities.GIGA_DRILL_BREAKER);
                message(player, settings, ExcavationMessages.shovelReady());
                sound(player, SoundEvents.ITEM_ARMOR_EQUIP_GOLD, 0.4F);
            } else if (result == ExcavationAbilityController.Preparation.LOWERED) {
                ExcavationAbilityEvents.cancelled(
                        player.getUuid(), CoreExcavationAbilities.GIGA_DRILL_BREAKER);
                message(player, settings, ExcavationMessages.shovelLowered());
            } else if (result instanceof ExcavationAbilityController.Preparation.Locked locked) {
                message(player, settings, ExcavationMessages.locked(locked.levelsRequired()));
            } else if (result instanceof ExcavationAbilityController.Preparation.Cooldown tired) {
                message(player, settings, ExcavationMessages.cooldown(tired.secondsRemaining()));
                sound(player, SoundEvents.ENTITY_VILLAGER_NO, 1.7F);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to prepare Giga Drill Breaker", exception);
        }
    }

    private static void activate(ServerPlayerEntity player, int level) {
        ExcavationSettings settings = FabricMmoFabricRuntime.excavationSettings();
        int cooldown = ExcavationPerks.cooldownSeconds(
                settings.gigaDrillCooldownSeconds(), player.getCommandSource(), PERMISSIONS);
        try {
            ExcavationAbilityController.Activation result =
                    FabricMmoFabricRuntime.excavationAbilities().activate(
                            player.getUuid(),
                            level,
                            settings,
                            cooldown,
                            ExcavationPerks.activationBonusSeconds(
                                    player.getCommandSource(), PERMISSIONS));
            if (result instanceof ExcavationAbilityController.Activation.Activated) {
                COOLDOWNS.add(player.getUuid());
                ExcavationAbilityEvents.activated(
                        player.getUuid(), CoreExcavationAbilities.GIGA_DRILL_BREAKER);
                GigaDrillAttributeBoost.update(player, settings.abilityEnchantBuff());
                message(player, settings, ExcavationMessages.activated());
                notifyNearby(player, settings, true);
                sound(player, SoundEvents.ITEM_TRIDENT_RIPTIDE_3, 1.0F);
            } else if (result instanceof ExcavationAbilityController.Activation.Locked locked) {
                message(player, settings, ExcavationMessages.locked(locked.levelsRequired()));
            } else if (result instanceof ExcavationAbilityController.Activation.Cooldown tired) {
                message(player, settings, ExcavationMessages.cooldown(tired.secondsRemaining()));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to activate Giga Drill Breaker", exception);
        }
    }

    private static void tick(ServerPlayerEntity player) {
        try {
            ExcavationSettings settings = FabricMmoFabricRuntime.excavationSettings();
            ExcavationAbilityController.TickResult result =
                    FabricMmoFabricRuntime.excavationAbilities().tick(player.getUuid());
            if (result.preparationExpired()) {
                ExcavationAbilityEvents.cancelled(
                        player.getUuid(), CoreExcavationAbilities.GIGA_DRILL_BREAKER);
                message(player, settings, ExcavationMessages.shovelLowered());
            }
            boolean active = FabricMmoFabricRuntime.excavationAbilities()
                    .isActive(player.getUuid());
            GigaDrillAttributeBoost.update(player, active ? settings.abilityEnchantBuff() : 0);
            if (result.abilityExpired()) {
                ExcavationAbilityEvents.expired(
                        player.getUuid(), CoreExcavationAbilities.GIGA_DRILL_BREAKER);
                message(player, settings, ExcavationMessages.expired());
                notifyNearby(player, settings, false);
            }
            int cooldown = ExcavationPerks.cooldownSeconds(
                    settings.gigaDrillCooldownSeconds(), player.getCommandSource(), PERMISSIONS);
            if (COOLDOWNS.contains(player.getUuid())
                    && FabricMmoFabricRuntime.excavationAbilities()
                            .cooldownRemaining(player.getUuid(), cooldown) == 0) {
                COOLDOWNS.remove(player.getUuid());
                message(player, settings, ExcavationMessages.refreshed());
                sound(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to tick Excavation abilities", exception);
        }
    }

    private static void message(
            ServerPlayerEntity player,
            ExcavationSettings settings,
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
            ExcavationSettings settings,
            boolean activated) {
        if (!settings.notifyNearbyPlayers()) {
            return;
        }
        for (ServerPlayerEntity other : player.getServerWorld().getPlayers()) {
            if (other != player && other.squaredDistanceTo(player) <= 100.0D) {
                other.sendMessage(activated
                        ? ExcavationMessages.activatedOther(player.getName().getString())
                        : ExcavationMessages.expiredOther(player.getName().getString()), false);
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
                null,
                player.getBlockPos(),
                sound,
                SoundCategory.PLAYERS,
                1.0F,
                pitch);
    }
}

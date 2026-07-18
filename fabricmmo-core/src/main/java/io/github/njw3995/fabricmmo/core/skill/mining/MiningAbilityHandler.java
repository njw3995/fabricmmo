package io.github.njw3995.fabricmmo.core.skill.mining;

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
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

/** Fabric event bridge for Super Breaker preparation, activation, expiry and cooldown feedback. */
public final class MiningAbilityHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final Set<UUID> SUPER_BREAKER_COOLDOWNS = new HashSet<>();
    private static final Set<UUID> BLAST_MINING_COOLDOWNS = new HashSet<>();

    private MiningAbilityHandler() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (hand != Hand.MAIN_HAND || world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !stack.isIn(ItemTags.PICKAXES) || !FabricMmoFabricRuntime.running()
                    || !(!SharedServerSystems.running()
                            || SharedServerSystems.require().sessions()
                                    .get(serverPlayer.getUuid()).abilityUse())
                    || FabricMmoFabricRuntime.isWorldBlacklisted(serverPlayer.getServerWorld())) {
                return TypedActionResult.pass(stack);
            }
            MiningSettings settings = FabricMmoFabricRuntime.miningSettings();
            if (settings.onlyActivateWhenSneaking() && !serverPlayer.isSneaking()) {
                return TypedActionResult.pass(stack);
            }
            if (!PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.MINING, true)
                    || !PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.MINING_SUPER_BREAKER, true)) {
                return TypedActionResult.pass(stack);
            }
            prepare(serverPlayer, settings);
            return TypedActionResult.pass(stack);
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (hand != Hand.MAIN_HAND || world.isClient()
                    || !(world instanceof ServerWorld serverWorld)
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !FabricMmoFabricRuntime.running()
                    || !(!SharedServerSystems.running()
                            || SharedServerSystems.require().sessions()
                                    .get(serverPlayer.getUuid()).abilityUse())
                    || FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)) {
                return ActionResult.PASS;
            }
            ItemStack tool = serverPlayer.getMainHandStack();
            net.minecraft.block.BlockState state = serverWorld.getBlockState(pos);
            boolean eligibleBlock = state.isIn(net.minecraft.registry.tag.BlockTags.PICKAXE_MINEABLE)
                    || FabricMmoFabricRuntime.miningXpFor(io.github.njw3995.fabricmmo.api.NamespacedId.parse(
                            net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString())) > 0;
            if (!tool.isIn(ItemTags.PICKAXES) || !eligibleBlock) {
                return ActionResult.PASS;
            }
            if (PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.MINING, true)
                    && PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.MINING_SUPER_BREAKER, true)) {
                activate(serverPlayer, FabricMmoFabricRuntime.miningSettings());
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!FabricMmoFabricRuntime.running()) {
                return;
            }
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tick(player);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                SuperBreakerAttributeBoost.cleanupInventory(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (FabricMmoFabricRuntime.running()) {
                UUID playerId = handler.player.getUuid();
                try {
                    if (FabricMmoFabricRuntime.miningAbilities().isSuperBreakerPrepared(playerId)
                            || FabricMmoFabricRuntime.miningAbilities().isSuperBreakerActive(playerId)) {
                        MiningAbilityEvents.cancelled(playerId, CoreMiningAbilities.SUPER_BREAKER);
                    }
                } catch (IOException exception) {
                    throw new UncheckedIOException("Unable to clear Mining ability state", exception);
                }
                SuperBreakerAttributeBoost.clear(handler.player);
                FabricMmoFabricRuntime.miningAbilities().removeTransient(playerId);
                FabricMmoFabricRuntime.playerSessionSettings().remove(playerId);
            }
        });
    }

    public static void reset() {
        SUPER_BREAKER_COOLDOWNS.clear();
        BLAST_MINING_COOLDOWNS.clear();
        SuperBreakerAttributeBoost.reset();
    }

    public static void trackBlastCooldown(UUID playerId) {
        BLAST_MINING_COOLDOWNS.add(playerId);
    }

    public static void restoreDroppedTool(ServerPlayerEntity player, ItemStack stack) {
        if (FabricMmoFabricRuntime.running()) {
            SuperBreakerAttributeBoost.restoreDroppedStack(player, stack);
        }
    }

    public static void refresh(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        try {
            if (FabricMmoFabricRuntime.miningAbilities().isSuperBreakerPrepared(playerId)
                    || FabricMmoFabricRuntime.miningAbilities().isSuperBreakerActive(playerId)) {
                MiningAbilityEvents.cancelled(playerId, CoreMiningAbilities.SUPER_BREAKER);
            }
            SuperBreakerAttributeBoost.clear(player);
            FabricMmoFabricRuntime.miningAbilities().reset(playerId);
            SUPER_BREAKER_COOLDOWNS.remove(playerId);
            BLAST_MINING_COOLDOWNS.remove(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to refresh Mining abilities", exception);
        }
    }

    public static void applyToolDamage(ServerPlayerEntity player) {
        MiningSettings settings = FabricMmoFabricRuntime.miningSettings();
        if (settings.abilityToolDamage() > 0
                && FabricMmoFabricRuntime.isSuperBreakerActive(player.getUuid())
                && PERMISSIONS.hasPermission(
                        player.getCommandSource(), PermissionNodes.MINING_SUPER_BREAKER, true)) {
            player.getMainHandStack().damage(settings.abilityToolDamage(), player, EquipmentSlot.MAINHAND);
        }
    }

    private static void prepare(ServerPlayerEntity player, MiningSettings settings) {
        int level = FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.MINING).level();
        try {
            MiningAbilityController.SuperBreakerPreparation result =
                    FabricMmoFabricRuntime.miningAbilities().prepareSuperBreaker(
                            player.getUuid(),
                            level,
                            settings,
                            MiningPerks.cooldownSeconds(
                                    settings.superBreakerCooldownSeconds(),
                                    player.getCommandSource(),
                                    PERMISSIONS));
            if (result == MiningAbilityController.SuperBreakerPreparation.READY) {
                MiningAbilityEvents.prepared(player.getUuid(), CoreMiningAbilities.SUPER_BREAKER);
                message(player, settings, MiningMessages.pickaxeReady());
                sound(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING, 0.4F);
            } else if (result == MiningAbilityController.SuperBreakerPreparation.LOWERED) {
                MiningAbilityEvents.cancelled(player.getUuid(), CoreMiningAbilities.SUPER_BREAKER);
                message(player, settings, MiningMessages.pickaxeLowered());
            } else if (result == MiningAbilityController.SuperBreakerPreparation.LOCKED) {
                message(player, settings, MiningMessages.locked(
                        "SUPER BREAKER", settings.superBreakerUnlockLevel()));
            } else if (result instanceof MiningAbilityController.SuperBreakerPreparation.Cooldown cooldown) {
                message(player, settings, MiningMessages.cooldown("Super Breaker", cooldown.secondsRemaining()));
                sound(player, SoundEvents.ENTITY_VILLAGER_NO, 1.7F);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to prepare Super Breaker", exception);
        }
    }

    private static void activate(ServerPlayerEntity player, MiningSettings settings) {
        int level = FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.MINING).level();
        try {
            MiningAbilityController.SuperBreakerActivation result =
                    FabricMmoFabricRuntime.miningAbilities().activateSuperBreaker(
                            player.getUuid(),
                            level,
                            settings,
                            MiningPerks.activationBonusSeconds(player.getCommandSource(), PERMISSIONS));
            if (result instanceof MiningAbilityController.SuperBreakerActivation.Activated activated) {
                MiningAbilityEvents.activated(player.getUuid(), CoreMiningAbilities.SUPER_BREAKER);
                SUPER_BREAKER_COOLDOWNS.add(player.getUuid());
                message(player, settings, MiningMessages.superBreakerActivated());
                notifyNearby(player, settings, true);
                SuperBreakerAttributeBoost.update(player, settings.abilityEnchantBuff());
                sound(player, SoundEvents.ITEM_TRIDENT_RIPTIDE_3, 1.0F);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to activate Super Breaker", exception);
        }
    }

    private static void tick(ServerPlayerEntity player) {
        try {
            MiningAbilityController.TickResult result =
                    FabricMmoFabricRuntime.miningAbilities().tick(player.getUuid());
            MiningSettings settings = FabricMmoFabricRuntime.miningSettings();
            if (result.preparationExpired()) {
                MiningAbilityEvents.cancelled(player.getUuid(), CoreMiningAbilities.SUPER_BREAKER);
                message(player, settings, MiningMessages.pickaxeLowered());
            }
            if (result.abilityExpired()) {
                SuperBreakerAttributeBoost.clear(player);
                MiningAbilityEvents.expired(player.getUuid(), CoreMiningAbilities.SUPER_BREAKER);
                message(player, settings, MiningMessages.superBreakerExpired());
                notifyNearby(player, settings, false);
            }
            UUID playerId = player.getUuid();
            if (FabricMmoFabricRuntime.miningAbilities().isSuperBreakerActive(playerId)) {
                SuperBreakerAttributeBoost.update(player, settings.abilityEnchantBuff());
            } else {
                SuperBreakerAttributeBoost.clear(player);
            }
            if (SUPER_BREAKER_COOLDOWNS.contains(playerId)
                    && FabricMmoFabricRuntime.miningAbilities()
                            .superBreakerCooldownRemaining(
                                    playerId,
                                    MiningPerks.cooldownSeconds(
                                            settings.superBreakerCooldownSeconds(),
                                            player.getCommandSource(),
                                            PERMISSIONS)) == 0) {
                SUPER_BREAKER_COOLDOWNS.remove(playerId);
                message(player, settings, MiningMessages.superBreakerRefreshed());
                sound(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F);
            }
            if (BLAST_MINING_COOLDOWNS.contains(playerId)
                    && FabricMmoFabricRuntime.miningAbilities()
                            .blastCooldownRemaining(
                                    playerId,
                                    MiningPerks.cooldownSeconds(
                                            settings.blastMiningCooldownSeconds(),
                                            player.getCommandSource(),
                                            PERMISSIONS)) == 0) {
                BLAST_MINING_COOLDOWNS.remove(playerId);
                message(player, settings, MiningMessages.blastRefreshed());
                sound(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to tick Mining abilities", exception);
        }
    }

    private static void message(ServerPlayerEntity player, MiningSettings settings, net.minecraft.text.Text text) {
        if (settings.abilityMessages()
                && (!SharedServerSystems.running()
                        || SharedServerSystems.require().sessions()
                                .get(player.getUuid()).notifications())) {
            player.sendMessage(text, true);
        }
    }

    private static void notifyNearby(
            ServerPlayerEntity player,
            MiningSettings settings,
            boolean activated) {
        if (!settings.notifyNearbyPlayers()) {
            return;
        }
        for (ServerPlayerEntity other : player.getServerWorld().getPlayers()) {
            if (other != player && other.squaredDistanceTo(player) <= 100.0D) {
                other.sendMessage(
                        activated
                                ? MiningMessages.superBreakerActivatedOther(player.getName().getString())
                                : MiningMessages.superBreakerExpiredOther(player.getName().getString()),
                        false);
            }
        }
    }

    private static void sound(ServerPlayerEntity player, SoundEvent sound, float pitch) {
        player.getWorld().playSound(
                null, player.getBlockPos(), sound, SoundCategory.PLAYERS, 1.0F, pitch);
    }

    private static void sound(
            ServerPlayerEntity player,
            net.minecraft.registry.entry.RegistryEntry<SoundEvent> sound,
            float pitch) {
        sound(player, sound.value(), pitch);
    }
}

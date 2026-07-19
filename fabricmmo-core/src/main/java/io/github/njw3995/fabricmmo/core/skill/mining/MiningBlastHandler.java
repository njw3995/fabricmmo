package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.TntEntity;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/** Remote TNT detonation entrypoint for Blast Mining. */
public final class MiningBlastHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();

    private MiningBlastHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND || world.isClient()
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !FabricMmoFabricRuntime.running()
                    || FabricMmoFabricRuntime.isWorldBlacklisted(serverPlayer.getServerWorld())
                    || !serverPlayer.isSneaking()
                    || world.getBlockState(hit.getBlockPos()).getBlock() != Blocks.TNT) {
                return ActionResult.PASS;
            }
            MiningSettings settings = FabricMmoFabricRuntime.miningSettings();
            ItemStack held = serverPlayer.getStackInHand(hand);
            int level = FabricMmoFabricRuntime.requireApi().progression()
                    .query(serverPlayer.getUuid(), CoreSkills.MINING).level();
            if (settings.blastRank(level) > 0
                    && isDetonator(held, settings)
                    && PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.MINING, true)
                    && PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.MINING_BLAST_MINING, true)) {
                // Upstream cancels a direct TNT click so remote detonation cannot be used at
                // point-blank range. The player must target the TNT with a right-click in air.
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack held = player.getStackInHand(hand);
            if (hand != Hand.MAIN_HAND || world.isClient()
                    || !(world instanceof ServerWorld serverWorld)
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !FabricMmoFabricRuntime.running()
                    || FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)
                    || !serverPlayer.isSneaking()) {
                return TypedActionResult.pass(held);
            }
            MiningSettings settings = FabricMmoFabricRuntime.miningSettings();
            if (!isDetonator(held, settings)
                    || !PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.MINING, true)
                    || !PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.MINING_BLAST_MINING, true)) {
                return TypedActionResult.pass(held);
            }
            HitResult hit = serverPlayer.raycast(settings.remoteDetonationDistance(), 0.0F, false);
            if (!(hit instanceof BlockHitResult blockHit)
                    || serverWorld.getBlockState(blockHit.getBlockPos()).getBlock() != Blocks.TNT) {
                return TypedActionResult.pass(held);
            }
            return detonate(serverPlayer, serverWorld, blockHit.getBlockPos(), held, settings);
        });
    }

    private static boolean isDetonator(ItemStack held, MiningSettings settings) {
        NamespacedId heldId = NamespacedId.parse(Registries.ITEM.getId(held.getItem()).toString());
        String configuredDetonator = settings.detonatorName();
        boolean configuredItem = configuredDetonator.indexOf(':') >= 0
                ? heldId.toString().equals(configuredDetonator)
                : heldId.path().equals(configuredDetonator);
        return held.isIn(ItemTags.PICKAXES) || configuredItem;
    }

    private static TypedActionResult<ItemStack> detonate(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            ItemStack held,
            MiningSettings settings) {
        String worldId = world.getRegistryKey().getValue().toString();
        if (!FabricMmoFabricRuntime.requireApi().protection().canBreak(
                player.getUuid(), worldId, pos.getX(), pos.getY(), pos.getZ())) {
            return TypedActionResult.pass(held);
        }
        int level = FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.MINING).level();
        try {
            MiningAbilityController.BlastActivation activation =
                    FabricMmoFabricRuntime.miningAbilities().activateBlastMining(
                            player.getUuid(),
                            level,
                            settings,
                            MiningPerks.cooldownSeconds(
                                    settings.blastMiningCooldownSeconds(),
                                    player.getCommandSource(),
                                    PERMISSIONS));
            if (activation instanceof MiningAbilityController.BlastActivation.Cooldown cooldown) {
                if (settings.abilityMessages()) {
                    player.sendMessage(MiningMessages.cooldown(
                            "Blast Mining", cooldown.secondsRemaining()), true);
                }
                return TypedActionResult.success(held, false);
            }
            if (activation instanceof MiningAbilityController.BlastActivation.Locked locked) {
                if (settings.abilityMessages()) {
                    player.sendMessage(MiningMessages.locked(
                            Math.max(0, locked.requiredLevel() - level)), true);
                }
                return TypedActionResult.success(held, false);
            }
            if (!(activation instanceof MiningAbilityController.BlastActivation.Activated activated)) {
                return TypedActionResult.pass(held);
            }

            FabricMmoFabricRuntime.clearPlayerPlaced(new io.github.njw3995.fabricmmo.core.block.BlockLocation(
                    worldId, pos.getX(), pos.getY(), pos.getZ()));
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
            TntEntity tnt = new TntEntity(
                    world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player);
            tnt.setFuse(0);
            boolean biggerBombs = level >= settings.biggerBombsUnlockLevel()
                    && PERMISSIONS.hasPermission(player.getCommandSource(),
                            PermissionNodes.MINING_BIGGER_BOMBS, true);
            boolean demolitionsExpertise = level >= settings.demolitionsExpertiseUnlockLevel()
                    && PERMISSIONS.hasPermission(player.getCommandSource(),
                            PermissionNodes.MINING_DEMOLITIONS_EXPERTISE, true);
            MiningBlastRegistry.track(tnt.getUuid(), player.getUuid(), activated.rank(),
                    biggerBombs, demolitionsExpertise);
            MiningAbilityEvents.activated(player.getUuid(), CoreMiningAbilities.BLAST_MINING);
            MiningAbilityHandler.trackBlastCooldown(player.getUuid());
            world.spawnEntity(tnt);
            if (settings.abilityMessages()) {
                player.sendMessage(MiningMessages.blastBoom(), true);
            }
            world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
            return TypedActionResult.success(held, false);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to activate Blast Mining", exception);
        }
    }
}

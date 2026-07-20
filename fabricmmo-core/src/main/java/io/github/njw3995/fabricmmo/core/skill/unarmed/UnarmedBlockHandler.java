package io.github.njw3995.fabricmmo.core.skill.unarmed;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Berserk block-cracking and instant-break translation of upstream BlockDamage behavior. */
public final class UnarmedBlockHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();

    private UnarmedBlockHandler() {
    }

    /** Called from continueMining after vanilla has performed normal reach/game-mode checks. */
    public static float modifyBreakingProgress(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockState state,
            BlockPos pos,
            float vanillaProgress) {
        if (!available(player, world, pos) || !isActive(player)) {
            return vanillaProgress;
        }
        UnarmedSettings settings = FabricMmoFabricRuntime.unarmedSettings();
        if (!UnarmedRuntimeHandler.isUnarmed(player.getMainHandStack(), settings)) {
            return vanillaProgress;
        }

        BlockState cracked = crackedState(state);
        if (cracked != null
                && settings.blockCrackerAllowed()
                && PERMISSIONS.hasPermission(
                        player.getCommandSource(), PermissionNodes.UNARMED_BLOCK_CRACKER, true)) {
            world.setBlockState(pos, cracked, 3);
            return 0.0F;
        }

        if (berserkInstantBreaks(state)) {
            playInstantBreakSound(player, world, pos, state, settings);
            return 1.0F;
        }
        return vanillaProgress;
    }

    /** Pinned upstream activates a prepared Berserk after processing the current block hit. */
    public static void activateAfterBlockAttack(
            ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (!available(player, world, pos)) {
            return;
        }
        UnarmedSettings settings = FabricMmoFabricRuntime.unarmedSettings();
        if (!UnarmedRuntimeHandler.isUnarmed(player.getMainHandStack(), settings)) {
            return;
        }
        UnarmedAbilityHandler.activateOnHit(player);
    }

    private static boolean available(
            ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (!FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)
                || !PERMISSIONS.hasPermission(
                        player.getCommandSource(), PermissionNodes.UNARMED, true)) {
            return false;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        return FabricMmoFabricRuntime.requireApi().protection().canBreak(
                player.getUuid(), worldId, pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean isActive(ServerPlayerEntity player) {
        try {
            return FabricMmoFabricRuntime.unarmedAbilities().isActive(player.getUuid());
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Berserk state", exception);
        }
    }

    private static boolean berserkInstantBreaks(BlockState state) {
        NamespacedId blockId = NamespacedId.parse(
                Registries.BLOCK.getId(state.getBlock()).toString());
        String path = blockId.path();
        return FabricMmoFabricRuntime.excavationXpFor(blockId) > 0
                || state.isOf(Blocks.SNOW)
                || path.contains("glass");
    }

    private static void playInstantBreakSound(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            BlockState state,
            UnarmedSettings settings) {
        boolean glass = Registries.BLOCK.getId(state.getBlock()).getPath().contains("glass");
        UnarmedSettings.SoundSetting configured = glass
                ? settings.glassSound() : settings.popSound();
        if (!configured.enabled()) {
            return;
        }
        Identifier id = Identifier.tryParse(configured.id());
        if (id == null) {
            return;
        }
        SoundEvent sound = Registries.SOUND_EVENT.get(id);
        float pitch = glass
                ? (float) configured.pitch()
                : ((world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7F + 1.0F)
                        * 2.0F;
        if (glass) {
            world.playSound(
                    null, pos, sound, SoundCategory.MASTER,
                    (float) configured.volume(), pitch);
        } else {
            player.playSoundToPlayer(
                    sound, SoundCategory.MASTER, (float) configured.volume(), pitch);
        }
    }

    static BlockState crackedState(BlockState state) {
        return switch (crackedPath(Registries.BLOCK.getId(state.getBlock()).getPath())) {
            case "cracked_stone_bricks" -> Blocks.CRACKED_STONE_BRICKS.getDefaultState();
            case "infested_cracked_stone_bricks" ->
                    Blocks.INFESTED_CRACKED_STONE_BRICKS.getDefaultState();
            case "cracked_deepslate_bricks" ->
                    Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
            case "cracked_deepslate_tiles" ->
                    Blocks.CRACKED_DEEPSLATE_TILES.getDefaultState();
            case "cracked_polished_blackstone_bricks" ->
                    Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            case "cracked_nether_bricks" -> Blocks.CRACKED_NETHER_BRICKS.getDefaultState();
            default -> null;
        };
    }

    static String crackedPath(String path) {
        return switch (path) {
            case "stone_bricks" -> "cracked_stone_bricks";
            case "infested_stone_bricks" -> "infested_cracked_stone_bricks";
            case "deepslate_bricks" -> "cracked_deepslate_bricks";
            case "deepslate_tiles" -> "cracked_deepslate_tiles";
            case "polished_blackstone_bricks" -> "cracked_polished_blackstone_bricks";
            case "nether_bricks" -> "cracked_nether_bricks";
            default -> "";
        };
    }
}

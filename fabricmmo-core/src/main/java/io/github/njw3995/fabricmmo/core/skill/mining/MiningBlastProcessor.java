package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.progression.PlayerProgressionContext;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Collection;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.Explosion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Upstream-shaped custom drop and XP processing for tracked Blast Mining explosions. */
public final class MiningBlastProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/BlastMining");

    private MiningBlastProcessor() {
    }

    public static boolean process(Explosion explosion) {
        if (!(explosion.getEntity() instanceof net.minecraft.entity.TntEntity tnt)
                || !(tnt.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        MiningBlastRegistry.BlastData data = MiningBlastRegistry.find(tnt.getUuid()).orElse(null);
        if (data == null) {
            return false;
        }
        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(data.ownerId());
        if (owner == null) {
            MiningBlastRegistry.remove(tnt.getUuid());
            return false;
        }

        if (FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return false;
        }
        MiningSettings settings = FabricMmoFabricRuntime.miningSettings();
        float vanillaYield = explosion.getPower() <= 0.0F ? 0.0F : 1.0F / explosion.getPower();
        float oreYield = Math.min(
                vanillaYield + vanillaYield * (float) settings.oreBonusFraction(data.rank()), 3.0F);
        int xp = 0;
        String worldId = world.getRegistryKey().getValue().toString();
        ItemStack held = owner.getMainHandStack();
        boolean pickaxe = held.isIn(ItemTags.PICKAXES);

        explosion.getAffectedBlocks().removeIf(pos -> !FabricMmoFabricRuntime.requireApi()
                .protection().canBreak(data.ownerId(), worldId, pos.getX(), pos.getY(), pos.getZ()));

        for (BlockPos pos : java.util.List.copyOf(explosion.getAffectedBlocks())) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
            boolean placed = FabricMmoFabricRuntime.isPlayerPlaced(location);
            FabricMmoFabricRuntime.clearPlayerPlaced(location);
            if (placed || MiningOreClassifier.illegalDrop(state)) {
                continue;
            }

            NamespacedId blockId = NamespacedId.parse(
                    Registries.BLOCK.getId(state.getBlock()).toString());
            int blockXp = FabricMmoFabricRuntime.miningXpFor(state);
            BlockEntity blockEntity = world.getBlockEntity(pos);
            boolean blastEligible = FabricMmoFabricRuntime.gatheringContentFor(CoreSkills.MINING, state)
                    .map(definition -> definition.activeAbility())
                    .orElseGet(() -> MiningOreClassifier.isOre(state));
            if (blockXp > 0 && blastEligible && blockEntity == null) {
                xp = Math.addExact(xp, blockXp);
                Collection<ItemStack> drops = pickaxe
                        ? Block.getDroppedStacks(state, world, pos, null, owner, held)
                        : blockItemDrop(state);
                emitYield(world, pos, drops, oreYield, settings.dropMultiplier(data.rank()),
                        settings.blastBonusDropsEnabled());
            } else if (world.getRandom().nextDouble() < 0.10D) {
                emit(world, pos, blockItemDrop(state));
            }
        }

        if (xp > 0) {
            XpAwardResult result = FabricMmoFabricRuntime.requireApi().progression().award(
                    new XpAwardRequest(
                            owner.getUuid(),
                            CoreSkills.MINING,
                            CoreXpSources.MINING_BLAST,
                            xp,
                            PlayerProgressionContext.enrich(
                                    owner,
                                    Map.of(
                                            "blastMining", "true",
                                            "rank", Integer.toString(data.rank()),
                                            "world", worldId,
                                            "upstreamReason", "PVE",
                                            "upstreamSource", "SELF"),
                                    FabricMmoFabricRuntime.progressionSettings(),
                                    CoreSkills.MINING)));
            if (result.status() != XpAwardResult.Status.APPLIED) {
                LOGGER.warn("Blast Mining XP award for {} was not applied: {}",
                        owner.getName().getString(), result.detail());
            } else if (result.newLevel() > result.oldLevel()) {
                owner.sendMessage(MiningMessages.levelUp(result.newLevel()), false);
            }
        }
        return true;
    }

    private static void emitYield(
            ServerWorld world,
            BlockPos pos,
            Collection<ItemStack> drops,
            float yield,
            int dropMultiplier,
            boolean bonusDropsEnabled) {
        float remaining = yield;
        while (remaining > 0.0F) {
            if (world.getRandom().nextFloat() < Math.min(remaining, 1.0F)) {
                emit(world, pos, drops);
                if (bonusDropsEnabled && world.getRandom().nextFloat() < 0.5F) {
                    for (int copy = 1; copy < dropMultiplier; copy++) {
                        emit(world, pos, drops);
                    }
                }
            }
            remaining = Math.max(remaining - 1.0F, 0.0F);
        }
    }

    private static Collection<ItemStack> blockItemDrop(BlockState state) {
        ItemStack stack = new ItemStack(state.getBlock().asItem());
        return stack.isEmpty() ? java.util.List.of() : java.util.List.of(stack);
    }

    private static void emit(ServerWorld world, BlockPos pos, Collection<ItemStack> drops) {
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity item = new ItemEntity(
                    world,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    stack.copy());
            item.setToDefaultPickupDelay();
            world.spawnEntity(item);
        }
    }

}

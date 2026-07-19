package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.progression.PlayerProgressionContext;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Herbalism XP, multi-block plant, Green Thumb replant, and placed-crop bridge. */
public final class HerbalismBlockBreakHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/Herbalism");
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final ThreadLocal<Set<BreakKey>> ACTIVE_BREAKS =
            ThreadLocal.withInitial(HashSet::new);
    private static final Map<BreakKey, PendingReplant> PENDING_REPLANTS = new HashMap<>();
    private static final List<ScheduledReplant> SCHEDULED_REPLANTS = new ArrayList<>();
    private static final Map<WorldPos, Integer> RECENT_REPLANTS = new HashMap<>();

    private HerbalismBlockBreakHandler() {
    }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)
                    || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return true;
            }
            WorldPos key = worldPos(serverWorld, pos);
            return !RECENT_REPLANTS.containsKey(key);
        });
        PlayerBlockBreakEvents.AFTER.register(HerbalismBlockBreakHandler::afterBlockBreak);
        ServerTickEvents.END_SERVER_TICK.register(server -> tickReplants());
    }

    private static void afterBlockBreak(
            World world,
            PlayerEntity player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld)
                || !(player instanceof ServerPlayerEntity serverPlayer)
                || !FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)) {
            return;
        }
        awardBeforeDrops(serverWorld, serverPlayer, pos, state, serverPlayer.getMainHandStack());
    }

    static void awardBeforeDrops(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            ItemStack tool) {
        if (!FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return;
        }
        BreakKey key = breakKey(world, player.getUuid(), pos);
        if (!ACTIVE_BREAKS.get().add(key)) {
            return;
        }
        prepareReplant(world, player, pos, state, tool, key);
        int budget = tallBudget(state);
        int awarded = 0;
        for (PlantSnapshot plant : connectedPlants(world, pos, state)) {
            int xp = xpFor(world, plant.pos(), plant.state());
            if (xp <= 0 || awarded >= budget) {
                continue;
            }
            int applied = Math.min(xp, budget - awarded);
            awardXp(world, player, plant.pos(), plant.state(), tool, applied);
            awarded += applied;
        }
    }

    public static void finishBlockBreak(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            boolean successful) {
        BreakKey key = breakKey(world, player.getUuid(), pos);
        Set<BreakKey> active = ACTIVE_BREAKS.get();
        boolean tracked = active.remove(key);
        PendingReplant pending;
        synchronized (PENDING_REPLANTS) {
            pending = PENDING_REPLANTS.remove(key);
            if (successful && pending != null) {
                SCHEDULED_REPLANTS.add(new ScheduledReplant(pending, 20));
            }
        }
        try {
            if (tracked && successful && FabricMmoFabricRuntime.running()) {
                FabricMmoFabricRuntime.clearPlayerPlaced(location(world, pos));
            }
        } finally {
            if (active.isEmpty()) {
                ACTIVE_BREAKS.remove();
            }
        }
    }

    static boolean eligibleForBonusDrops(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state) {
        String path = path(state);
        int xp = FabricMmoFabricRuntime.herbalismXpFor(id(state));
        if (xp <= 0 || player.isCreative()) {
            return false;
        }
        boolean placed = FabricMmoFabricRuntime.isPlayerPlaced(location(world, pos));
        IntProperty age = HerbalismPlantRules.ageProperty(state);
        if (placed) {
            return age != null
                    && !HerbalismPlantRules.isBizarreAgeable(path)
                    && HerbalismPlantRules.isMature(state, path);
        }
        return age == null
                || HerbalismPlantRules.isBizarreAgeable(path)
                || HerbalismPlantRules.isMature(state, path);
    }

    private static int xpFor(ServerWorld world, BlockPos pos, BlockState state) {
        int configured = FabricMmoFabricRuntime.herbalismXpFor(id(state));
        if (configured <= 0) {
            return 0;
        }
        String path = path(state);
        boolean placed = FabricMmoFabricRuntime.isPlayerPlaced(location(world, pos));
        IntProperty age = HerbalismPlantRules.ageProperty(state);
        if (placed) {
            return age != null
                    && !HerbalismPlantRules.isBizarreAgeable(path)
                    && HerbalismPlantRules.isMature(state, path) ? configured : 0;
        }
        if (age != null
                && !HerbalismPlantRules.isBizarreAgeable(path)
                && !HerbalismPlantRules.isMature(state, path)) {
            return 0;
        }
        return configured;
    }

    private static void awardXp(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            ItemStack tool,
            int xp) {
        boolean permission = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.HERBALISM, true);
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        boolean protection = api.protection().canBreak(
                player.getUuid(), world.getRegistryKey().getValue().toString(),
                pos.getX(), pos.getY(), pos.getZ());
        if (!permission || !protection || player.isCreative()) {
            return;
        }
        NamespacedId blockId = id(state);
        NamespacedId toolId = NamespacedId.parse(
                Registries.ITEM.getId(tool.getItem()).toString());
        XpAwardResult result = api.progression().award(new XpAwardRequest(
                player.getUuid(),
                CoreSkills.HERBALISM,
                CoreXpSources.HERBALISM_BLOCK_BREAK,
                xp,
                PlayerProgressionContext.enrich(
                        player,
                        Map.of(
                                "block", blockId.toString(),
                                "tool", toolId.toString(),
                                "world", world.getRegistryKey().getValue().toString(),
                                "x", Integer.toString(pos.getX()),
                                "y", Integer.toString(pos.getY()),
                                "z", Integer.toString(pos.getZ()),
                                "greenTerra", Boolean.toString(
                                        HerbalismAbilityHandler.isActive(player.getUuid())),
                                "upstreamReason", "PVE",
                                "upstreamSource", "SELF"),
                        FabricMmoFabricRuntime.progressionSettings(),
                        CoreSkills.HERBALISM)));
        if (result.status() != XpAwardResult.Status.APPLIED) {
            LOGGER.warn("Herbalism XP award for {} was not applied: {}",
                    player.getName().getString(), result.detail());
        } else if (result.newLevel() > result.oldLevel()) {
            player.sendMessage(Text.literal("Herbalism increased to " + result.newLevel() + "."), false);
        }
    }

    private static void prepareReplant(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            ItemStack tool,
            BreakKey key) {
        String path = path(state);
        HerbalismSettings settings = FabricMmoFabricRuntime.herbalismSettings();
        IntProperty age = HerbalismPlantRules.ageProperty(state);
        if (age == null || !settings.replantEnabled(path) || player.isSneaking()) {
            return;
        }
        boolean validTool = tool.isIn(ItemTags.HOES)
                || (path.equals("cocoa") && tool.isIn(ItemTags.AXES));
        if (!validTool) {
            return;
        }
        int level = FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.HERBALISM).level();
        if (settings.greenThumbRank(level) <= 0
                || !PERMISSIONS.hasPermission(
                        player.getCommandSource(),
                        PermissionNodes.herbalismGreenThumbPlant(path),
                        true)) {
            return;
        }
        boolean greenTerra = HerbalismAbilityHandler.isActive(player.getUuid());
        boolean lucky = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.HERBALISM_LUCKY, false);
        if (!greenTerra && !HerbalismProbability.roll(
                world.getRandom().nextDouble(), settings.greenThumbChance(level, lucky))) {
            return;
        }
        Item seed = seedFor(path);
        if (seed == null || !HerbalismInventory.removeOne(player, seed)) {
            return;
        }
        boolean mature = HerbalismPlantRules.isMature(state, path);
        int stage = settings.greenThumbStage(level, greenTerra);
        int desired = mature ? HerbalismPlantRules.replantAge(path, stage, greenTerra) : 0;
        int max = age.getValues().stream().mapToInt(Integer::intValue).max().orElse(0);
        BlockState replanted = state.with(age, Math.min(max, Math.max(0, desired)));
        synchronized (PENDING_REPLANTS) {
            PENDING_REPLANTS.put(key,
                    new PendingReplant(world, pos.toImmutable(), replanted));
        }
        world.playSound(
                null,
                pos,
                SoundEvents.ITEM_BOTTLE_EMPTY,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F);
    }

    private static Item seedFor(String path) {
        return switch (path) {
            case "carrots" -> Items.CARROT;
            case "wheat" -> Items.WHEAT_SEEDS;
            case "nether_wart" -> Items.NETHER_WART;
            case "potatoes" -> Items.POTATO;
            case "beetroot", "beetroots" -> Items.BEETROOT_SEEDS;
            case "cocoa" -> Items.COCOA_BEANS;
            case "sweet_berry_bush" -> Items.SWEET_BERRIES;
            default -> null;
        };
    }

    private static void tickReplants() {
        synchronized (PENDING_REPLANTS) {
            tickRecentReplants();
            for (int index = SCHEDULED_REPLANTS.size() - 1; index >= 0; index--) {
                ScheduledReplant scheduled = SCHEDULED_REPLANTS.get(index);
                if (scheduled.ticksRemaining() > 1) {
                    SCHEDULED_REPLANTS.set(index,
                            new ScheduledReplant(scheduled.replant(), scheduled.ticksRemaining() - 1));
                    continue;
                }
                PendingReplant replant = scheduled.replant();
                if (replant.world().getBlockState(replant.pos()).isAir()) {
                    replant.world().setBlockState(
                            replant.pos(), replant.state(), Block.NOTIFY_ALL);
                    FabricMmoFabricRuntime.markPlayerPlaced(location(replant.world(), replant.pos()));
                    RECENT_REPLANTS.put(worldPos(replant.world(), replant.pos()), 10);
                    playReplantEffect(replant.world(), replant.pos());
                }
                SCHEDULED_REPLANTS.remove(index);
            }
        }
    }

    private static void tickRecentReplants() {
        RECENT_REPLANTS.replaceAll((ignored, ticks) -> ticks - 1);
        RECENT_REPLANTS.values().removeIf(ticks -> ticks <= 0);
    }

    private static void playReplantEffect(ServerWorld world, BlockPos pos) {
        world.spawnParticles(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                9,
                0.35D,
                0.20D,
                0.35D,
                0.0D);
        world.playSound(
                null,
                pos,
                SoundEvents.ENTITY_ITEM_PICKUP,
                SoundCategory.PLAYERS,
                1.0F,
                ((world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7F + 1.0F)
                        * 2.0F);
    }

    private static int tallBudget(BlockState origin) {
        HerbalismSettings settings = FabricMmoFabricRuntime.herbalismSettings();
        String path = path(origin);
        if (!settings.limitXpOnTallPlants() || !HerbalismPlantRules.limitedTallPlant(path)) {
            return Integer.MAX_VALUE;
        }
        int limit = switch (path) {
            case "cactus", "sugar_cane" -> 3;
            case "bamboo" -> 20;
            case "kelp", "kelp_plant" -> 26;
            case "chorus_plant" -> 22;
            default -> Integer.MAX_VALUE;
        };
        int perBlock = FabricMmoFabricRuntime.herbalismXpFor(id(origin));
        return limit == Integer.MAX_VALUE ? limit : limit * Math.max(0, perBlock);
    }

    private static List<PlantSnapshot> connectedPlants(
            ServerWorld world,
            BlockPos origin,
            BlockState originState) {
        String path = path(originState);
        ArrayList<PlantSnapshot> result = new ArrayList<>();
        result.add(new PlantSnapshot(origin.toImmutable(), originState));
        if (path.equals("cactus") || path.equals("sugar_cane") || path.equals("bamboo")
                || path.equals("kelp") || path.equals("kelp_plant")
                || path.equals("twisting_vines_plant")) {
            collectVertical(world, origin, 1, path, result, 32);
        } else if (path.equals("weeping_vines_plant") || path.equals("cave_vines_plant")) {
            collectVertical(world, origin, -1, path, result, 32);
        } else if (path.equals("chorus_plant")) {
            collectChorus(world, origin, result);
        }
        return List.copyOf(result);
    }

    private static void collectVertical(
            ServerWorld world,
            BlockPos origin,
            int direction,
            String family,
            List<PlantSnapshot> result,
            int limit) {
        for (int distance = 1; distance < limit; distance++) {
            BlockPos pos = origin.add(0, direction * distance, 0);
            BlockState state = world.getBlockState(pos);
            String path = path(state);
            if (!path.equals(family)
                    && !(family.equals("kelp") && path.equals("kelp_plant"))) {
                break;
            }
            result.add(new PlantSnapshot(pos.toImmutable(), state));
        }
    }

    private static void collectChorus(
            ServerWorld world,
            BlockPos origin,
            List<PlantSnapshot> result) {
        ArrayList<BlockPos> queue = new ArrayList<>();
        HashSet<BlockPos> visited = new HashSet<>();
        queue.add(origin.toImmutable());
        for (int index = 0; index < queue.size() && visited.size() < 22; index++) {
            BlockPos pos = queue.get(index);
            if (!visited.add(pos)) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (block != Blocks.CHORUS_PLANT && block != Blocks.CHORUS_FLOWER) {
                continue;
            }
            if (!pos.equals(origin)) {
                result.add(new PlantSnapshot(pos, state));
            }
            queue.add(pos.up());
            queue.add(pos.down());
            queue.add(pos.north());
            queue.add(pos.south());
            queue.add(pos.east());
            queue.add(pos.west());
        }
    }


    public static void reset() {
        ACTIVE_BREAKS.remove();
        synchronized (PENDING_REPLANTS) {
            PENDING_REPLANTS.clear();
            SCHEDULED_REPLANTS.clear();
            RECENT_REPLANTS.clear();
        }
    }

    private static NamespacedId id(BlockState state) {
        return NamespacedId.parse(Registries.BLOCK.getId(state.getBlock()).toString());
    }

    private static String path(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).getPath();
    }

    private static BlockLocation location(ServerWorld world, BlockPos pos) {
        return new BlockLocation(
                world.getRegistryKey().getValue().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    private static BreakKey breakKey(ServerWorld world, UUID player, BlockPos pos) {
        return new BreakKey(player, location(world, pos));
    }

    private static WorldPos worldPos(ServerWorld world, BlockPos pos) {
        return new WorldPos(world.getRegistryKey().getValue().toString(), pos.toImmutable());
    }

    private record BreakKey(UUID playerId, BlockLocation location) {
    }

    private record PendingReplant(ServerWorld world, BlockPos pos, BlockState state) {
    }

    private record ScheduledReplant(PendingReplant replant, int ticksRemaining) {
    }

    private record PlantSnapshot(BlockPos pos, BlockState state) {
    }

    private record WorldPos(String worldId, BlockPos pos) {
    }
}

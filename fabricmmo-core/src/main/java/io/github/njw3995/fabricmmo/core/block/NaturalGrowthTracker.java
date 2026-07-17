package io.github.njw3995.fabricmmo.core.block;

import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Fabric equivalent of mcMMO's generic BlockGrowEvent eligibility cleanup. */
public final class NaturalGrowthTracker {
    private static final Set<String> PROPERTY_GROWTH_BLOCKS = Set.of(
            "minecraft:wheat",
            "minecraft:carrots",
            "minecraft:potatoes",
            "minecraft:beetroots",
            "minecraft:nether_wart",
            "minecraft:cocoa",
            "minecraft:sweet_berry_bush",
            "minecraft:melon_stem",
            "minecraft:pumpkin_stem",
            "minecraft:cactus",
            "minecraft:sugar_cane",
            "minecraft:bamboo",
            "minecraft:kelp",
            "minecraft:weeping_vines",
            "minecraft:twisting_vines",
            "minecraft:cave_vines",
            "minecraft:cave_vines_plant",
            "minecraft:chorus_flower",
            "minecraft:mangrove_propagule",
            "minecraft:torchflower_crop",
            "minecraft:pitcher_crop",
            "minecraft:turtle_egg",
            "minecraft:sniffer_egg",
            "minecraft:sea_pickle");

    private static final Set<String> GROWTH_NUMBER_PROPERTIES = Set.of(
            "age", "stage", "hatch", "pickles", "flower_amount");
    private static final Set<String> GROWTH_BOOLEAN_PROPERTIES = Set.of("berries");

    private static final Set<String> GROWTH_CREATED_BLOCKS = Set.of(
            "minecraft:cactus",
            "minecraft:sugar_cane",
            "minecraft:bamboo",
            "minecraft:kelp",
            "minecraft:kelp_plant",
            "minecraft:weeping_vines",
            "minecraft:weeping_vines_plant",
            "minecraft:twisting_vines",
            "minecraft:twisting_vines_plant",
            "minecraft:cave_vines",
            "minecraft:cave_vines_plant",
            "minecraft:chorus_flower",
            "minecraft:chorus_plant",
            "minecraft:melon",
            "minecraft:pumpkin",
            "minecraft:pointed_dripstone",
            "minecraft:small_amethyst_bud",
            "minecraft:medium_amethyst_bud",
            "minecraft:large_amethyst_bud",
            "minecraft:amethyst_cluster");

    private NaturalGrowthTracker() {
    }

    public static void blockChanged(
            ServerWorld world,
            BlockPos pos,
            BlockState previous,
            BlockState current) {
        if (!FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)
                || previous.equals(current)
                || !isGrowthTransition(previous, current)) {
            return;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        FabricMmoFabricRuntime.clearPlayerPlaced(new BlockLocation(
                worldId, pos.getX(), pos.getY(), pos.getZ()));
    }

    /**
     * Recognizes vanilla mutations that correspond to Bukkit's BlockGrowEvent without treating
     * every random-tick mutation (fire aging, copper oxidation, leaf decay, ice melt, etc.) as
     * growth.
     */
    static boolean isGrowthTransition(BlockState previous, BlockState current) {
        return isGrowthTransition(
                Registries.BLOCK.getId(previous.getBlock()).toString(),
                propertyValues(previous),
                isReplaceableForGrowth(previous),
                Registries.BLOCK.getId(current.getBlock()).toString(),
                propertyValues(current));
    }

    /** Data-only form used by unit tests without bootstrapping the Minecraft registries. */
    static boolean isGrowthTransition(
            String previousBlock,
            Map<String, ? extends Comparable<?>> previousProperties,
            boolean previousReplaceable,
            String currentBlock,
            Map<String, ? extends Comparable<?>> currentProperties) {
        if (previousBlock.equals(currentBlock)) {
            return PROPERTY_GROWTH_BLOCKS.contains(currentBlock)
                    && hasIncreasingGrowthProperty(previousProperties, currentProperties);
        }
        if (isAmethystGrowth(previousBlock, currentBlock)
                || isStemAttachment(previousBlock, currentBlock)
                || isGrowingTipTransition(previousBlock, currentBlock)
                || (previousBlock.equals("minecraft:bamboo_sapling")
                        && currentBlock.equals("minecraft:bamboo"))) {
            return true;
        }
        return previousReplaceable && GROWTH_CREATED_BLOCKS.contains(currentBlock);
    }

    private static Map<String, Comparable<?>> propertyValues(BlockState state) {
        Map<String, Comparable<?>> values = new HashMap<>();
        state.getEntries().forEach((property, value) -> values.put(property.getName(), value));
        return values;
    }

    private static boolean hasIncreasingGrowthProperty(
            Map<String, ? extends Comparable<?>> previous,
            Map<String, ? extends Comparable<?>> current) {
        for (Map.Entry<String, ? extends Comparable<?>> entry : current.entrySet()) {
            Comparable<?> oldValue = previous.get(entry.getKey());
            Comparable<?> newValue = entry.getValue();
            if (oldValue == null || oldValue.equals(newValue)) {
                continue;
            }
            if (GROWTH_NUMBER_PROPERTIES.contains(entry.getKey())
                    && oldValue instanceof Integer oldNumber
                    && newValue instanceof Integer newNumber
                    && newNumber > oldNumber) {
                return true;
            }
            if (GROWTH_BOOLEAN_PROPERTIES.contains(entry.getKey())
                    && oldValue instanceof Boolean oldBoolean
                    && newValue instanceof Boolean newBoolean
                    && !oldBoolean
                    && newBoolean) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReplaceableForGrowth(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty();
    }

    private static boolean isAmethystGrowth(String previous, String current) {
        return (previous.equals("minecraft:small_amethyst_bud")
                        && current.equals("minecraft:medium_amethyst_bud"))
                || (previous.equals("minecraft:medium_amethyst_bud")
                        && current.equals("minecraft:large_amethyst_bud"))
                || (previous.equals("minecraft:large_amethyst_bud")
                        && current.equals("minecraft:amethyst_cluster"));
    }

    private static boolean isStemAttachment(String previous, String current) {
        return (previous.equals("minecraft:melon_stem")
                        && current.equals("minecraft:attached_melon_stem"))
                || (previous.equals("minecraft:pumpkin_stem")
                        && current.equals("minecraft:attached_pumpkin_stem"));
    }

    private static boolean isGrowingTipTransition(String previous, String current) {
        return (previous.equals("minecraft:kelp") && current.equals("minecraft:kelp_plant"))
                || (previous.equals("minecraft:weeping_vines")
                        && current.equals("minecraft:weeping_vines_plant"))
                || (previous.equals("minecraft:twisting_vines")
                        && current.equals("minecraft:twisting_vines_plant"))
                || (previous.equals("minecraft:cave_vines")
                        && current.equals("minecraft:cave_vines_plant"))
                || (previous.equals("minecraft:chorus_flower")
                        && current.equals("minecraft:chorus_plant"));
    }
}

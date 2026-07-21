package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.skill.gathering.ConfiguredBlockXpTable;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;

/** 1.21.1-applicable Tree Feller block classification from mcMMO MaterialMapStore. */
public final class WoodcuttingBlockClassifier {
    private static final Set<String> EXTRA_TREE_PARTS = Set.of(
            "minecraft:mangrove_roots",
            "minecraft:nether_wart_block",
            "minecraft:warped_wart_block",
            "minecraft:brown_mushroom_block",
            "minecraft:red_mushroom_block");

    private WoodcuttingBlockClassifier() {
    }

    public static TreeFellerSearch.Kind kind(
            BlockState state,
            ConfiguredBlockXpTable xpTable) {
        NamespacedId id = id(state);
        if (FabricMmoFabricRuntime.running()) {
            var extension = FabricMmoFabricRuntime.gatheringContentFor(CoreSkills.WOODCUTTING, state);
            if (extension.isPresent()) {
                return extension.get().activeAbility()
                        ? TreeFellerSearch.Kind.WOOD_XP
                        : TreeFellerSearch.Kind.OTHER;
            }
        }
        if (xpTable.xpFor(id) > 0) {
            return TreeFellerSearch.Kind.WOOD_XP;
        }
        if (isNonWoodTreePart(state)) {
            return TreeFellerSearch.Kind.NON_WOOD_TREE_PART;
        }
        return TreeFellerSearch.Kind.OTHER;
    }

    public static boolean isNonWoodTreePart(BlockState state) {
        NamespacedId id = id(state);
        return state.isIn(BlockTags.LEAVES)
                || state.isIn(BlockTags.WART_BLOCKS)
                || EXTRA_TREE_PARTS.contains(id.toString());
    }

    public static NamespacedId id(BlockState state) {
        return NamespacedId.parse(Registries.BLOCK.getId(state.getBlock()).toString());
    }
}

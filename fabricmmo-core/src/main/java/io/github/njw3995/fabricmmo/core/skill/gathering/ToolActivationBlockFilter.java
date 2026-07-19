package io.github.njw3995.fabricmmo.core.skill.gathering;

import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;

/**
 * Fabric equivalent of mcMMO's tool-activation blacklist for Minecraft 1.21.1.
 *
 * <p>The filter prevents right-clicking interactable blocks or axe-transformable wood from
 * preparing a gathering super ability. In particular, this preserves upstream's behavior that
 * stripping a log does not also ready Tree Feller.</p>
 */
public final class ToolActivationBlockFilter {
    private static final Set<String> EXPLICIT = Set.of(
            "bell",
            "barrel",
            "blast_furnace",
            "campfire",
            "soul_campfire",
            "cartography_table",
            "composter",
            "grindstone",
            "lectern",
            "loom",
            "scaffolding",
            "smoker",
            "stonecutter",
            "sweet_berry_bush",
            "smithing_table",
            "lodestone",
            "respawn_anchor",
            "chiseled_bookshelf",
            "brewing_stand",
            "bookshelf",
            "cake",
            "dispenser",
            "enchanting_table",
            "furnace",
            "jukebox",
            "lever",
            "note_block",
            "crafting_table",
            "beacon",
            "anvil",
            "chipped_anvil",
            "damaged_anvil",
            "dropper",
            "hopper",
            "trapped_chest",
            "ender_chest");

    private ToolActivationBlockFilter() {
    }

    /** Returns whether a right-click against this block may prepare a tool ability. */
    public static boolean canActivateTools(BlockState state) {
        return canActivateTools(Registries.BLOCK.getId(state.getBlock()).getPath());
    }

    static boolean canActivateTools(String path) {
        return !EXPLICIT.contains(path)
                && !hasAnySuffix(path,
                        "_button",
                        "_trapdoor",
                        "_fence_gate",
                        "_bed",
                        "_pressure_plate",
                        "_chest",
                        "_door",
                        "_fence",
                        "_sign",
                        "_hanging_sign",
                        "_shulker_box",
                        "_log",
                        "_wood",
                        "_stem",
                        "_hyphae")
                && !path.equals("bamboo_block")
                && !path.equals("stripped_bamboo_block");
    }

    private static boolean hasAnySuffix(String value, String... suffixes) {
        for (String suffix : suffixes) {
            if (value.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}

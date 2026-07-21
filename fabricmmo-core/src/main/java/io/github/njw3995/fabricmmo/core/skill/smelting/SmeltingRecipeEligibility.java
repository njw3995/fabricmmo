package io.github.njw3995.fabricmmo.core.skill.smelting;

import io.github.njw3995.fabricmmo.core.skill.mining.MiningOreClassifier;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.world.ServerWorld;

/** Upstream ItemUtils.isSmelted equivalent for the vanilla furnace XP multiplier. */
public final class SmeltingRecipeEligibility {
    private SmeltingRecipeEligibility() {
    }

    public static boolean isOreSmeltingOutput(ServerWorld world, ItemStack output) {
        if (output.isEmpty()) {
            return false;
        }
        for (RecipeEntry<SmeltingRecipe> entry
                : world.getRecipeManager().listAllOfType(RecipeType.SMELTING)) {
            SmeltingRecipe recipe = entry.value();
            ItemStack result = recipe.getResult(world.getRegistryManager());
            if (!result.isOf(output.getItem())) {
                continue;
            }
            for (Ingredient ingredient : recipe.getIngredients()) {
                for (ItemStack input : ingredient.getMatchingStacks()) {
                    if (input.getItem() instanceof BlockItem blockItem
                            && MiningOreClassifier.isOre(blockItem.getBlock().getDefaultState())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

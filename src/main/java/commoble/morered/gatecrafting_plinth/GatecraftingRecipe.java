package commoble.morered.gatecrafting_plinth;

import javax.annotation.Nonnull;

import commoble.morered.RecipeRegistrar;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/**
 * Wrapper around vanilla shapeless recipes to change the recipe type
 **/
public class GatecraftingRecipe extends ShapelessRecipe {
    public GatecraftingRecipe(ShapelessRecipe baseRecipe) {
        super(baseRecipe.getId(), baseRecipe.getGroup(), baseRecipe.getResultItem(), baseRecipe.getIngredients());
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegistrar.GATECRAFTING_RECIPE_TYPE;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistrar.GATECRAFTING_SERIALIZER.get();
    }

    public static boolean doesPlayerHaveIngredients(Inventory playerInventory,
                                                    @Nonnull Recipe<CraftingContainer> recipe) {
        // assumes that the ingredient list doesn't have two of the same ingredients
        // e.g. two stacks of redstone
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        for (Ingredient ingredient : ingredients) {
            int remainingItems = ingredient.getItems()[0].getCount();
            int playerSlots = playerInventory.getContainerSize();
            for (int playerSlot = 0; playerSlot < playerSlots && remainingItems > 0; playerSlot++) {
                ItemStack stackInSlot = playerInventory.getItem(playerSlot);
                if (ingredient.test(stackInSlot)) {
                    remainingItems -= stackInSlot.getCount();
                }
            }
            if (remainingItems > 0) {
                return false;
            }
        }
        return true;
    }


}

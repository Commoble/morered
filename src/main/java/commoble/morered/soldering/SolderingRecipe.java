package commoble.morered.soldering;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import commoble.morered.MoreRed;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/** Wrapper around vanilla shapeless recipes to change the recipe type **/
public class SolderingRecipe extends ShapelessRecipe
{
	public SolderingRecipe(ShapelessRecipe baseRecipe)
	{
		super(baseRecipe.getId(), baseRecipe.getGroup(), baseRecipe.category(), baseRecipe.getResultItem(RegistryAccess.EMPTY), baseRecipe.getIngredients());
	}

	@Override
	public RecipeType<?> getType()
	{
		return MoreRed.get().solderingRecipeType.get();
	}

	@Override
	public RecipeSerializer<?> getSerializer()
	{
		return MoreRed.get().solderingSerializer.get();
	}
	
	public static boolean doesPlayerHaveIngredients(Inventory playerInventory, @Nonnull Recipe<CraftingContainer> recipe)
	{
		// assumes that the ingredient list doesn't have two of the same ingredients
		// e.g. two stacks of redstone
		NonNullList<Ingredient> ingredients = recipe.getIngredients();
		for (Ingredient ingredient : ingredients)
		{
			int remainingItems = ingredient.getItems()[0].getCount();
			int playerSlots = playerInventory.getContainerSize();
			for (int playerSlot = 0; playerSlot < playerSlots && remainingItems > 0; playerSlot++)
			{
				ItemStack stackInSlot = playerInventory.getItem(playerSlot);
				if (ingredient.test(stackInSlot))
				{
					remainingItems -= stackInSlot.getCount();
				}
			}
			if (remainingItems > 0)
			{
				return false;
			}
		}
		return true;
	}

	public static List<Recipe<CraftingContainer>> getAllSolderingRecipes(RecipeManager manager, RegistryAccess registries)
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<ResourceLocation, Recipe<CraftingContainer>> map = (Map)manager.recipes.getOrDefault(MoreRed.get().solderingRecipeType.get(), Collections.emptyMap());
			return map.entrySet().stream()
			.map(Entry::getValue)
			.sorted(Comparator.comparing(recipe -> recipe.getResultItem(registries).getDescriptionId()))
			.collect(Collectors.toList());
	}
}

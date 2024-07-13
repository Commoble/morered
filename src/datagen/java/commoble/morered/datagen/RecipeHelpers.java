package commoble.morered.datagen;

import java.util.List;
import java.util.Map;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public class RecipeHelpers
{
	public static ShapelessRecipe shapeless(Item result, int count, CraftingBookCategory category, List<Ingredient> ingredients)
	{
		return new ShapelessRecipe(
			"", // recipe book group (not used)
			category,
			new ItemStack(result, count),
			NonNullList.copyOf(ingredients));
	}
	
	public static ShapedRecipe shaped(Item result, int count, CraftingBookCategory category, List<String> pattern, Map<Character,Ingredient> key)
	{
		return new ShapedRecipe(
			"", // recipe book group (not used),
			category,
			ShapedRecipePattern.of(key, pattern),
			new ItemStack(result, count),
			true);
	}
}
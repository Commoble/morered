package commoble.morered.datagen;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

public class RecipeHelpers
{
	public static FinishedRecipe shapeless(ResourceLocation recipeId, Item result, int count, List<Ingredient> ingredients)
	{
		return new ShapelessRecipeBuilder.Result(
			recipeId,
			result,
			count,
			"", // recipe book group (not used)
			ingredients,
			null, // advancement
			null
		)
		{
			@Override
			public JsonObject serializeAdvancement()
			{
				return null;
			}
		};
	}
	
	public static FinishedRecipe shaped(ResourceLocation recipeId, Item result, int count, List<String> pattern, Map<Character,Ingredient> key)
	{
		return new ShapedRecipeBuilder.Result(
			recipeId,
			result,
			count,
			"", // recipe book group (not used)
			pattern,
			key,
			null, // advancement
			null)
		{
			@Override
			public JsonObject serializeAdvancement()
			{
				return null;
			}
		};
	}
}